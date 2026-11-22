package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Buy & Sell, deliberately without payments: list -> chat -> meet -> mark sold.
 * The price is informational only and is settled in person.
 */
@Service
@RequiredArgsConstructor
public class SaleService {
    public static final List<SaleStatus> ACTIVE = List.of(SaleStatus.LISTED, SaleStatus.RESERVED);

    private final SaleListingRepository sales;
    private final BookRepository books;
    private final MessageRepository msgs;
    private final UserService users;
    private final CommunityService communities;
    private final ChatService chat;
    private final Mapper mapper;

    @Transactional
    public SaleListing ensureListing(Book b) {
        return sales.findFirstByBookIdAndStatusIn(b.getId(), ACTIVE).orElseGet(() -> {
            SaleListing s = new SaleListing();
            s.setBook(b);
            s.setSeller(b.getOwner());
            return sales.save(s);
        });
    }

    @Transactional
    public void cancelListingForBook(Book b) {
        sales.findFirstByBookIdAndStatusIn(b.getId(), ACTIVE).ifPresent(s -> {
            s.setStatus(SaleStatus.CANCELLED);
            sales.save(s);
        });
    }

    private SaleListing load(Long id) {
        return sales.findById(id).orElseThrow(() -> ApiException.notFound("Listing not found"));
    }

    private SaleListing mineAsSeller(Long meId, Long id) {
        SaleListing s = load(id);
        if (!s.getSeller().getId().equals(meId)) throw ApiException.forbidden("Only the seller can do that.");
        return s;
    }

    @Transactional
    public Dto.SaleDto create(Long meId, Long bookId) {
        Book b = books.findById(bookId).orElseThrow(() -> ApiException.notFound("Book not found"));
        if (!b.getOwner().getId().equals(meId)) throw ApiException.forbidden("Only the owner can list a book for sale.");
        if (b.isArchived()) throw ApiException.conflict("This book is no longer on the shelf.");
        if (b.getListingType() == ListingType.BORROW) {
            throw ApiException.badRequest("Change the book's listing type to SALE or BOTH first.");
        }
        return mapper.sale(ensureListing(b));
    }

    @Transactional(readOnly = true)
    public List<Dto.SaleDto> search(Long meId, String q, String scope) {
        Set<Long> ids = new HashSet<>(communities.visibleIds(users.require(meId), scope));
        String needle = q == null ? "" : q.trim().toLowerCase();
        return sales.findByStatusInOrderByCreatedAtDesc(ACTIVE).stream()
                .filter(s -> !s.getBook().isArchived() && ids.contains(s.getBook().getCommunity().getId()))
                .filter(s -> needle.isEmpty() || s.getBook().getTitle().toLowerCase().contains(needle)
                        || s.getBook().getAuthor().toLowerCase().contains(needle))
                .map(mapper::sale).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.SaleDto> mine(Long meId) {
        return sales.findBySellerIdOrderByCreatedAtDesc(meId).stream().map(mapper::sale).toList();
    }

    @Transactional(readOnly = true)
    public Dto.SaleDto get(Long meId, Long id) { return mapper.sale(load(id)); }

    /** Buyer says hello: opens (or returns) the one chat thread between this buyer and the seller. */
    @Transactional
    public Dto.ThreadDto interest(Long meId, Long id) {
        SaleListing s = load(id);
        if (s.getSeller().getId().equals(meId)) throw ApiException.badRequest("This is your own listing.");
        if (s.getStatus() == SaleStatus.RESERVED && (s.getBuyer() == null || !s.getBuyer().getId().equals(meId))) {
            throw ApiException.conflict("This book is reserved for someone else.");
        }
        if (s.getStatus() != SaleStatus.LISTED && s.getStatus() != SaleStatus.RESERVED) {
            throw ApiException.conflict("This listing is closed.");
        }
        if (!msgs.existsBySaleListingIdAndThreadUserId(id, meId)) {
            chat.sendSale(meId, id, meId, "Hi! I'm interested in \u201C" + s.getBook().getTitle() + "\u201D. Is it still available?");
        }
        return thread(id, meId);
    }

    private Dto.ThreadDto thread(Long saleId, Long buyerId) {
        List<Message> list = msgs.findBySaleListingIdAndThreadUserIdOrderBySentAtAsc(saleId, buyerId);
        Message last = list.isEmpty() ? null : list.get(list.size() - 1);
        return new Dto.ThreadDto(mapper.user(users.require(buyerId)), last == null ? null : last.getContent(),
                last == null ? null : last.getSentAt());
    }

    @Transactional(readOnly = true)
    public List<Dto.ThreadDto> threads(Long meId, Long id) {
        SaleListing s = load(id);
        if (s.getSeller().getId().equals(meId)) {
            return msgs.threadUserIds(id).stream().map(b -> thread(id, b)).toList();
        }
        return msgs.existsBySaleListingIdAndThreadUserId(id, meId) ? List.of(thread(id, meId)) : List.of();
    }

    @Transactional
    public Dto.SaleDto reserve(Long meId, Long id, Dto.ReserveReq req) {
        SaleListing s = mineAsSeller(meId, id);
        if (s.getStatus() != SaleStatus.LISTED) throw ApiException.conflict("Only a listed book can be reserved.");
        if (!msgs.existsBySaleListingIdAndThreadUserId(id, req.buyerId())) {
            throw ApiException.badRequest("That person has not messaged you about this book.");
        }
        BigDecimal price = req.agreedPrice() != null ? req.agreedPrice() : s.getBook().getPrice();
        s.setBuyer(users.require(req.buyerId()));
        s.setAgreedPrice(price);
        s.setStatus(SaleStatus.RESERVED);
        sales.save(s);
        chat.systemSale(s, req.buyerId(), "Reserved for you" + (price == null ? "." : " at an agreed price of " + price.toPlainString() + "."));
        return mapper.sale(s);
    }

    @Transactional
    public Dto.SaleDto release(Long meId, Long id) {
        SaleListing s = mineAsSeller(meId, id);
        if (s.getStatus() != SaleStatus.RESERVED) throw ApiException.conflict("Only a reserved book can be released.");
        Long buyerId = s.getBuyer() == null ? null : s.getBuyer().getId();
        s.setBuyer(null);
        s.setAgreedPrice(null);
        s.setStatus(SaleStatus.LISTED);
        sales.save(s);
        if (buyerId != null) chat.systemSale(s, buyerId, "The reservation was released.");
        return mapper.sale(s);
    }

    @Transactional
    public Dto.SaleDto sold(Long meId, Long id) {
        SaleListing s = mineAsSeller(meId, id);
        if (s.getStatus() != SaleStatus.RESERVED) throw ApiException.conflict("Reserve the book for a buyer before marking it sold.");
        s.setStatus(SaleStatus.SOLD);
        Book b = s.getBook();
        b.setAvailable(false);
        b.setArchived(true);
        books.save(b);
        sales.save(s);
        chat.systemSale(s, s.getBuyer().getId(), "Marked as sold. Happy reading!");
        return mapper.sale(s);
    }


    /**
     * DEMO ONLY: simulates a successful checkout. No money is collected and no payment gateway is called.
     * The authenticated user becomes the buyer; the listing and book are then marked sold/archived.
     */
    @Transactional
    public Dto.SaleDto simulatePayment(Long buyerId, Long id) {
        SaleListing s = load(id);
        if (s.getSeller().getId().equals(buyerId)) {
            throw ApiException.badRequest("You cannot buy your own book.");
        }
        if (s.getStatus() == SaleStatus.RESERVED
                && (s.getBuyer() == null || !s.getBuyer().getId().equals(buyerId))) {
            throw ApiException.conflict("This book is reserved for another buyer.");
        }
        if (s.getStatus() != SaleStatus.LISTED && s.getStatus() != SaleStatus.RESERVED) {
            throw ApiException.conflict("This listing is no longer available.");
        }
        if (s.getBook().getPrice() == null || s.getBook().getPrice().signum() <= 0) {
            throw ApiException.badRequest("The seller has not set a valid book price.");
        }

        User buyer = users.require(buyerId);
        s.setBuyer(buyer);
        s.setAgreedPrice(s.getBook().getPrice());
        s.setDemoPaymentReference("DEMO-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        s.setDemoPaymentStatus("SIMULATED_SUCCESS");
        s.setDemoPaidAt(java.time.Instant.now());
        s.setStatus(SaleStatus.SOLD);

        Book book = s.getBook();
        book.setAvailable(false);
        book.setArchived(true);
        books.save(book);
        sales.save(s);
        chat.systemSale(s, buyerId, "Demo checkout completed successfully. This was a simulation only; no real money was charged.");
        return mapper.sale(s);
    }

    @Transactional
    public Dto.SaleDto cancel(Long meId, Long id) {
        SaleListing s = mineAsSeller(meId, id);
        if (!ACTIVE.contains(s.getStatus())) throw ApiException.conflict("This listing is already closed.");
        s.setStatus(SaleStatus.CANCELLED);
        sales.save(s);
        return mapper.sale(s);
    }
}
