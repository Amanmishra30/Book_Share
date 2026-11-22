package com.bookshare.web;

import com.bookshare.security.AuthUser;
import com.bookshare.service.*;
import com.bookshare.web.Dto;
import com.bookshare.domain.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookController {
    private final BookService books;
    private final ConditionCheckService checks;
    private final RecommendationService recommendations;

    @GetMapping("/books")
    public List<Dto.BookDto> list(@AuthenticationPrincipal AuthUser me,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String genre,
                                  @RequestParam(required = false) BookCondition condition,
                                  @RequestParam(required = false) ListingType listingType,
                                  @RequestParam(required = false) Boolean available,
                                  @RequestParam(defaultValue = "own") String scope) {
        return books.list(me.id(), q, genre, condition, listingType, available, scope);
    }

    @GetMapping("/books/mine")
    public List<Dto.BookDto> mine(@AuthenticationPrincipal AuthUser me) { return books.mine(me.id()); }

    @GetMapping("/books/{id}")
    public Dto.BookDto get(@PathVariable Long id) { return books.get(id); }

    @PostMapping(value = "/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dto.BookDto create(@AuthenticationPrincipal AuthUser me,
                              @RequestPart("book") @Valid Dto.BookReq book,
                              @RequestPart(value = "cover", required = false) MultipartFile cover,
                              @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return books.create(me.id(), book, cover, photos);
    }

    @PutMapping(value = "/books/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dto.BookDto update(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                              @RequestPart("book") @Valid Dto.BookReq book,
                              @RequestPart(value = "cover", required = false) MultipartFile cover) {
        return books.update(me.id(), id, book, cover);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> remove(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        books.remove(me.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books/{id}/condition-checks")
    public List<Dto.ConditionCheckDto> conditionChecks(@PathVariable Long id) { return checks.forBook(id); }

    @PostMapping(value = "/books/{id}/condition-checks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dto.ConditionCheckDto addListingPhotos(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                                  @RequestPart("photos") List<MultipartFile> photos) {
        return checks.addForBook(me.id(), id, photos);
    }

    @GetMapping("/books/{id}/recommendations")
    public List<Dto.BookDto> alsoBorrowed(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return recommendations.forBook(me.id(), id);
    }

    @GetMapping("/recommendations")
    public List<Dto.BookDto> forMe(@AuthenticationPrincipal AuthUser me) { return recommendations.forMe(me.id()); }
}
