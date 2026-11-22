package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Photo-based condition verification. A check is a small fixed set of photos (front cover, back cover, spine,
 * fanned pages, plus one optional extra), taken at LISTING, PICKUP and RETURN. 3 to 5 photos per check.
 */
@Service
@RequiredArgsConstructor
public class ConditionCheckService {
    public static final int MIN_PHOTOS = 3;
    public static final int MAX_PHOTOS = 5;

    private final ConditionCheckRepository checks;
    private final BorrowRequestRepository reqs;
    private final BookRepository books;
    private final UserService users;
    private final StorageService storage;
    private final ObjectProvider<DamageDetector> detector;
    private final Mapper mapper;

    private static final Set<BorrowStatus> PICKUP_OK = EnumSet.of(BorrowStatus.ACCEPTED, BorrowStatus.PICKED_UP);
    private static final Set<BorrowStatus> RETURN_OK = EnumSet.of(BorrowStatus.BORROWED, BorrowStatus.RETURN_DUE, BorrowStatus.RETURNED);

    static List<MultipartFile> validate(List<MultipartFile> files) {
        List<MultipartFile> real = files == null ? List.of() : files.stream().filter(f -> !f.isEmpty()).toList();
        if (real.size() < MIN_PHOTOS || real.size() > MAX_PHOTOS) {
            throw ApiException.badRequest("Please upload 3 to 5 photos: front cover, back cover, spine and a fanned-page shot.");
        }
        real.forEach(StorageService::requireImage);
        return real;
    }

    private ConditionCheck build(Book book, BorrowRequest req, CheckStage stage, User by, List<MultipartFile> files) {
        List<MultipartFile> real = validate(files);
        ConditionCheck c = new ConditionCheck();
        c.setBook(book);
        c.setBorrowRequest(req);
        c.setStage(stage);
        c.setLoggedBy(by);
        for (MultipartFile f : real) c.getPhotoUrls().add(storage.store(f));
        if (stage != CheckStage.LISTING) {
            DamageDetector d = detector.getIfAvailable();
            if (d != null) c.setAiNotes(d.analyse(c.getPhotoUrls()));
        }
        return checks.save(c);
    }

    @Transactional
    public Dto.ConditionCheckDto addForBook(Long meId, Long bookId, List<MultipartFile> files) {
        Book b = books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book not found"));
        if (!b.getOwner().getId().equals(meId)) throw ApiException.forbidden("Only the owner can add listing photos.");
        return mapper.check(build(b, null, CheckStage.LISTING, users.require(meId), files));
    }

    /** Used by BookService when photos are uploaded together with a new book. */
    @Transactional
    public ConditionCheck createListingCheck(Book b, User owner, List<MultipartFile> files) {
        return build(b, null, CheckStage.LISTING, owner, files);
    }

    @Transactional
    public Dto.ConditionCheckDto addForRequest(Long meId, Long reqId, CheckStage stage, List<MultipartFile> files) {
        BorrowRequest r = reqs.findById(reqId).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (!r.getBorrower().getId().equals(meId) && !r.getOwner().getId().equals(meId)) {
            throw ApiException.forbidden("Only the borrower or the owner can add photos.");
        }
        if (stage == CheckStage.PICKUP) {
            if (!PICKUP_OK.contains(r.getStatus())) throw ApiException.conflict("Pickup photos can be added once the request is accepted and until the book is received.");
        } else if (stage == CheckStage.RETURN) {
            if (!RETURN_OK.contains(r.getStatus())) throw ApiException.conflict("Return photos can be added while the book is borrowed or just returned.");
        } else {
            throw ApiException.badRequest("stage must be PICKUP or RETURN");
        }
        return mapper.check(build(r.getBook(), r, stage, users.require(meId), files));
    }

    @Transactional(readOnly = true)
    public List<Dto.ConditionCheckDto> forBook(Long bookId) {
        return checks.findByBookIdOrderByCreatedAtAsc(bookId).stream().map(mapper::check).toList();
    }

    /** Evidence for a transaction: the book's listing photos plus every pickup/return check of that request. */
    @Transactional(readOnly = true)
    public List<Dto.ConditionCheckDto> evidenceFor(BorrowRequest r) {
        List<ConditionCheck> all = new ArrayList<>();
        checks.findByBookIdOrderByCreatedAtAsc(r.getBook().getId()).stream()
                .filter(c -> c.getStage() == CheckStage.LISTING).forEach(all::add);
        all.addAll(checks.findByBorrowRequestIdOrderByCreatedAtAsc(r.getId()));
        all.sort(Comparator.comparing(ConditionCheck::getCreatedAt));
        return all.stream().map(mapper::check).toList();
    }
}
