package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookService {
    public static final List<BorrowStatus> ACTIVE_LOAN =
            List.of(BorrowStatus.ACCEPTED, BorrowStatus.PICKED_UP, BorrowStatus.BORROWED, BorrowStatus.RETURN_DUE);

    private final BookRepository books;
    private final SaleListingRepository sales;
    private final BorrowRequestRepository reqs;
    private final CommunityService communities;
    private final UserService users;
    private final TrustService trust;
    private final StorageService storage;
    private final ConditionCheckService checks;
    private final SaleService saleService;
    private final Mapper mapper;

    /** Converts books to DTOs, attaching each book's active sale listing (if any) in one query. */
    @Transactional(readOnly = true)
    public List<Dto.BookDto> dtos(Collection<Book> list) {
        if (list.isEmpty()) return List.of();
        Map<Long, SaleListing> byBook = new HashMap<>();
        sales.findByBookIdInAndStatusIn(list.stream().map(Book::getId).toList(), SaleService.ACTIVE)
                .forEach(s -> byBook.put(s.getBook().getId(), s));
        return list.stream().map(b -> mapper.book(b, byBook.get(b.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.BookDto> list(Long meId, String q, String genre, BookCondition condition, ListingType type,
                                  Boolean available, String scope) {
        User me = users.require(meId);
        List<Long> ids = communities.visibleIds(me, scope);
        Specification<Book> spec = (root, cq, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(cb.isFalse(root.get("archived")));
            p.add(root.get("community").get("id").in(ids));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("author")), like)));
            }
            if (genre != null && !genre.isBlank()) p.add(cb.equal(cb.lower(root.get("genre")), genre.trim().toLowerCase()));
            if (condition != null) p.add(cb.equal(root.get("bookCondition"), condition));
            if (type == ListingType.SALE) p.add(root.get("listingType").in(List.of(ListingType.SALE, ListingType.BOTH)));
            else if (type == ListingType.BORROW) p.add(root.get("listingType").in(List.of(ListingType.BORROW, ListingType.BOTH)));
            else if (type == ListingType.BOTH) p.add(cb.equal(root.get("listingType"), ListingType.BOTH));
            if (available != null) p.add(cb.equal(root.get("available"), available));
            return cb.and(p.toArray(new Predicate[0]));
        };
        return dtos(books.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public List<Dto.BookDto> mine(Long meId) {
        return dtos(books.findByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(meId));
    }

    @Transactional(readOnly = true)
    public Dto.BookDto get(Long id) {
        Book b = require(id);
        return dtos(List.of(b)).get(0);
    }

    public Book require(Long id) {
        return books.findById(id).orElseThrow(() -> ApiException.notFound("Book not found"));
    }

    private static void checkPrice(ListingType t, BigDecimal price) {
        if (t != ListingType.BORROW && price == null) throw ApiException.badRequest("Please set a price for books listed for sale.");
    }

    @Transactional
    public Dto.BookDto create(Long meId, Dto.BookReq req, MultipartFile cover, List<MultipartFile> photos) {
        User me = users.require(meId);
        trust.assertCanLend(me);
        checkPrice(req.listingType(), req.price());
        List<MultipartFile> realPhotos = photos == null ? List.of() : photos.stream().filter(f -> !f.isEmpty()).toList();
        if (!realPhotos.isEmpty()) ConditionCheckService.validate(realPhotos);   // fail before anything is stored

        Book b = new Book();
        b.setOwner(me);
        b.setCommunity(me.getCommunity());
        apply(b, req);
        if (cover != null && !cover.isEmpty()) b.setCoverUrl(storage.store(cover));
        b = books.save(b);
        if (!realPhotos.isEmpty()) checks.createListingCheck(b, me, realPhotos);
        if (b.getListingType() != ListingType.BORROW) saleService.ensureListing(b);
        return dtos(List.of(b)).get(0);
    }

    private void apply(Book b, Dto.BookReq req) {
        b.setTitle(req.title().trim());
        b.setAuthor(req.author().trim());
        b.setGenre(req.genre() == null || req.genre().isBlank() ? null : req.genre().trim());
        b.setEdition(req.edition() == null || req.edition().isBlank() ? null : req.edition().trim());
        b.setBookCondition(req.condition());
        b.setDescription(req.description());
        b.setListingType(req.listingType());
        b.setPrice(req.listingType() == ListingType.BORROW ? null : req.price());
        if (req.lendingDurationDays() != null) b.setLendingDurationDays(req.lendingDurationDays());
    }

    @Transactional
    public Dto.BookDto update(Long meId, Long id, Dto.BookReq req, MultipartFile cover) {
        Book b = require(id);
        if (!b.getOwner().getId().equals(meId)) throw ApiException.forbidden("Only the owner can edit this book.");
        checkPrice(req.listingType(), req.price());
        apply(b, req);
        if (cover != null && !cover.isEmpty()) b.setCoverUrl(storage.store(cover));
        if (req.available() != null) {
            if (req.available() && reqs.existsByBookIdAndStatusIn(id, ACTIVE_LOAN)) {
                throw ApiException.conflict("This book is out on loan, so it cannot be marked available yet.");
            }
            b.setAvailable(req.available());
        }
        books.save(b);
        if (b.getListingType() == ListingType.BORROW) saleService.cancelListingForBook(b);
        else saleService.ensureListing(b);
        return dtos(List.of(b)).get(0);
    }

    @Transactional
    public void remove(Long meId, Long id) {
        Book b = require(id);
        if (!b.getOwner().getId().equals(meId)) throw ApiException.forbidden("Only the owner can remove this book.");
        if (reqs.existsByBookIdAndStatusIn(id, ACTIVE_LOAN)) throw ApiException.conflict("This book is out on loan.");
        saleService.cancelListingForBook(b);
        b.setArchived(true);
        b.setAvailable(false);
        books.save(b);
    }
}
