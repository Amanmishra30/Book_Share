package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.bookshare.domain.BorrowStatus.*;

/**
 * The borrow/return state machine. Status can only move along the transitions in NEXT, and the photo
 * checks are mandatory gates: PICKED_UP needs a PICKUP ConditionCheck, RETURNED needs a RETURN one.
 *
 * PENDING -> ACCEPTED | REJECTED            (owner)
 * ACCEPTED -> PICKED_UP                      (owner hands the book over; needs PICKUP photos)
 * PICKED_UP -> BORROWED                      (borrower confirms receipt)
 * BORROWED -> RETURN_DUE                     (automatic, when return_due_at has passed; sends a reminder)
 * BORROWED | RETURN_DUE -> RETURNED          (borrower returns; needs RETURN photos)
 * RETURNED -> COMPLETED                      (owner confirms; unlocks ratings)
 */
@Service
@RequiredArgsConstructor
public class BorrowService {
    private static final Map<BorrowStatus, Set<BorrowStatus>> NEXT = new EnumMap<>(BorrowStatus.class);
    static {
        NEXT.put(PENDING, EnumSet.of(ACCEPTED, REJECTED));
        NEXT.put(ACCEPTED, EnumSet.of(PICKED_UP));
        NEXT.put(PICKED_UP, EnumSet.of(BORROWED));
        NEXT.put(BORROWED, EnumSet.of(RETURN_DUE, RETURNED));
        NEXT.put(RETURN_DUE, EnumSet.of(RETURNED));
        NEXT.put(RETURNED, EnumSet.of(COMPLETED));
        NEXT.put(REJECTED, EnumSet.noneOf(BorrowStatus.class));
        NEXT.put(COMPLETED, EnumSet.noneOf(BorrowStatus.class));
    }

    private final BorrowRequestRepository reqs;
    private final BookRepository books;
    private final ConditionCheckRepository checks;
    private final ReportRepository reports;
    private final RatingRepository ratings;
    private final UserService users;
    private final TrustService trust;
    private final ReputationService reputation;
    private final ConditionCheckService checkService;
    private final ChatService chat;
    private final Mapper mapper;

    @Value("${app.trust.grace-days}")
    private int graceDays;

    private BorrowRequest load(Long id) {
        return reqs.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
    }

    private BorrowRequest asParty(Long meId, Long id) {
        BorrowRequest r = load(id);
        if (!r.getBorrower().getId().equals(meId) && !r.getOwner().getId().equals(meId)) {
            throw ApiException.forbidden("This exchange belongs to someone else.");
        }
        return r;
    }

    private void move(BorrowRequest r, BorrowStatus to) {
        if (!NEXT.get(r.getStatus()).contains(to)) {
            throw ApiException.conflict("A request that is " + r.getStatus() + " cannot move to " + to + ".");
        }
        r.setStatus(to);
    }

    private BorrowRequest asOwner(Long meId, Long id) {
        BorrowRequest r = load(id);
        if (!r.getOwner().getId().equals(meId)) throw ApiException.forbidden("Only the owner can do that.");
        return r;
    }

    private BorrowRequest asBorrower(Long meId, Long id) {
        BorrowRequest r = load(id);
        if (!r.getBorrower().getId().equals(meId)) throw ApiException.forbidden("Only the borrower can do that.");
        return r;
    }

    // ---------------------------------------------------------------- queries
    Dto.RequestDto toDto(BorrowRequest r, Long meId) {
        Long otherId = meId.equals(r.getOwner().getId()) ? r.getBorrower().getId() : r.getOwner().getId();
        Instant reportable = r.getReturnDueAt() == null ? null : r.getReturnDueAt().plus(graceDays, ChronoUnit.DAYS);
        return new Dto.RequestDto(r.getId(), mapper.book(r.getBook(), null), mapper.user(r.getBorrower()), mapper.user(r.getOwner()),
                r.getStatus().name(), r.getNote(), r.getRequestedAt(), r.getAcceptedAt(), r.getPickedUpAt(), r.getReturnDueAt(),
                r.getReturnedAt(), r.getCompletedAt(), r.getReminderSentAt(), reportable,
                ratings.existsByBorrowRequestIdAndRaterId(r.getId(), meId),
                ratings.existsByBorrowRequestIdAndRaterId(r.getId(), otherId),
                reputation.compliance(r.getBorrower().getId()), reputation.compliance(r.getOwner().getId()));
    }

    @Transactional(readOnly = true)
    public List<Dto.RequestDto> mine(Long meId, String role) {
        List<BorrowRequest> list = "owner".equalsIgnoreCase(role)
                ? reqs.findByOwnerIdOrderByRequestedAtDesc(meId)
                : reqs.findByBorrowerIdOrderByRequestedAtDesc(meId);
        return list.stream().map(r -> toDto(r, meId)).toList();
    }

    @Transactional(readOnly = true)
    public Dto.RequestDetailDto detail(Long meId, Long id) {
        BorrowRequest r = asParty(meId, id);
        return new Dto.RequestDetailDto(toDto(r, meId), checkService.evidenceFor(r),
                reports.findByBorrowRequestIdOrderByCreatedAtDesc(id).stream().map(x -> mapper.report(x, null)).toList(),
                ratings.findByBorrowRequestId(id).stream().map(mapper::rating).toList());
    }

    // ---------------------------------------------------------------- transitions
    @Transactional
    public Dto.RequestDto create(Long meId, Dto.CreateRequestReq req) {
        User me = users.require(meId);
        trust.assertCanBorrow(me);
        Book b = books.findById(req.bookId()).orElseThrow(() -> ApiException.notFound("Book not found"));
        if (b.isArchived()) throw ApiException.conflict("This book is no longer on the shelf.");
        if (b.getOwner().getId().equals(meId)) throw ApiException.badRequest("This is your own book.");
        if (b.getListingType() == ListingType.SALE) throw ApiException.badRequest("This book is for sale only, not for lending.");
        if (!b.isAvailable()) throw ApiException.conflict("This book is not available right now.");
        List<BorrowStatus> open = List.of(PENDING, ACCEPTED, PICKED_UP, BORROWED, RETURN_DUE);
        if (reqs.existsByBookIdAndBorrowerIdAndStatusIn(b.getId(), meId, open)) {
            throw ApiException.conflict("You already have an open request for this book.");
        }
        BorrowRequest r = new BorrowRequest();
        r.setBook(b);
        r.setBorrower(me);
        r.setOwner(b.getOwner());
        r.setNote(req.note() == null || req.note().isBlank() ? null : req.note().trim());
        r = reqs.save(r);
        chat.systemRequest(r, me.getName() + " asked to borrow \u201C" + b.getTitle() + "\u201D.");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto accept(Long meId, Long id) {
        BorrowRequest r = asOwner(meId, id);
        trust.assertCanLend(r.getOwner());
        if (r.getStatus() != PENDING) throw ApiException.conflict("Only a pending request can be accepted.");
        if (reqs.existsByBookIdAndStatusIn(r.getBook().getId(), BookService.ACTIVE_LOAN) || !r.getBook().isAvailable()) {
            throw ApiException.conflict("This book is already promised to someone else or marked unavailable.");
        }
        Instant now = Instant.now();
        r.setAcceptedAt(now);
        r.setReturnDueAt(now.plus(r.getBook().getLendingDurationDays(), ChronoUnit.DAYS));
        move(r, ACCEPTED);
        chat.systemRequest(r, "Accepted! Return by " + r.getReturnDueAt() + ". Upload pickup photos when you meet.");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto reject(Long meId, Long id) {
        BorrowRequest r = asOwner(meId, id);
        move(r, REJECTED);
        chat.systemRequest(r, "The owner declined this request.");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto pickup(Long meId, Long id) {
        BorrowRequest r = asOwner(meId, id);
        if (!checks.existsByBorrowRequestIdAndStage(id, CheckStage.PICKUP)) {
            throw ApiException.conflict("Upload the pickup photos (cover, spine, fanned pages) before handing the book over.");
        }
        move(r, PICKED_UP);
        r.setPickedUpAt(Instant.now());
        r.getBook().setAvailable(false);
        books.save(r.getBook());
        chat.systemRequest(r, "The book was handed over. Borrower, please confirm you received it.");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto receive(Long meId, Long id) {
        BorrowRequest r = asBorrower(meId, id);
        move(r, BORROWED);
        chat.systemRequest(r, "Received. Happy reading! Due back on " + r.getReturnDueAt() + ".");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto returnBook(Long meId, Long id) {
        BorrowRequest r = asBorrower(meId, id);
        if (!checks.existsByBorrowRequestIdAndStage(id, CheckStage.RETURN)) {
            throw ApiException.conflict("Upload the return photos (cover, spine, fanned pages) before marking the book returned.");
        }
        move(r, RETURNED);
        r.setReturnedAt(Instant.now());
        r.getBook().setAvailable(true);
        books.save(r.getBook());
        reputation.recalc(r.getBorrower().getId());
        chat.systemRequest(r, "The book was returned. Owner, please confirm it came back fine.");
        return toDto(r, meId);
    }

    @Transactional
    public Dto.RequestDto complete(Long meId, Long id) {
        BorrowRequest r = asOwner(meId, id);
        move(r, COMPLETED);
        r.setCompletedAt(Instant.now());
        reputation.recalc(r.getBorrower().getId());
        reputation.recalc(r.getOwner().getId());
        chat.systemRequest(r, "Exchange completed. You can now rate each other.");
        return toDto(r, meId);
    }

    /** Called by the scheduler: books past their due date become RETURN_DUE and a reminder is posted in the chat. */
    @Transactional
    public int sweepOverdue() {
        List<BorrowRequest> late = reqs.findByStatusAndReturnDueAtBefore(BORROWED, Instant.now());
        for (BorrowRequest r : late) {
            move(r, RETURN_DUE);
            r.setReminderSentAt(Instant.now());
            reputation.recalc(r.getBorrower().getId());
            chat.systemRequest(r, "Reminder: this book was due on " + r.getReturnDueAt()
                    + ". Please return it soon, or talk to the owner here.");
        }
        return late.size();
    }
}
