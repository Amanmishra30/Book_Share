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
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {
    private final BorrowService borrow;
    private final ConditionCheckService checks;
    private final ReportService reports;
    private final RatingService ratings;

    @PostMapping
    public Dto.RequestDto create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody Dto.CreateRequestReq req) {
        return borrow.create(me.id(), req);
    }

    @GetMapping
    public List<Dto.RequestDto> mine(@AuthenticationPrincipal AuthUser me, @RequestParam(defaultValue = "borrower") String role) {
        return borrow.mine(me.id(), role);
    }

    @GetMapping("/{id}")
    public Dto.RequestDetailDto detail(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.detail(me.id(), id); }

    @PostMapping("/{id}/accept")
    public Dto.RequestDto accept(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.accept(me.id(), id); }

    @PostMapping("/{id}/reject")
    public Dto.RequestDto reject(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.reject(me.id(), id); }

    @PostMapping("/{id}/pickup")
    public Dto.RequestDto pickup(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.pickup(me.id(), id); }

    @PostMapping("/{id}/receive")
    public Dto.RequestDto receive(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.receive(me.id(), id); }

    @PostMapping("/{id}/return")
    public Dto.RequestDto returned(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.returnBook(me.id(), id); }

    @PostMapping("/{id}/complete")
    public Dto.RequestDto complete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return borrow.complete(me.id(), id); }

    @PostMapping(value = "/{id}/condition-checks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dto.ConditionCheckDto addPhotos(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                           @RequestParam CheckStage stage,
                                           @RequestPart("photos") List<MultipartFile> photos) {
        return checks.addForRequest(me.id(), id, stage, photos);
    }

    @GetMapping("/{id}/reports")
    public List<Dto.ReportDto> listReports(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) { return reports.forRequest(me.id(), id); }

    @PostMapping(value = "/{id}/reports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Dto.ReportDto report(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                @RequestParam ReportCategory category,
                                @RequestParam(required = false) String details,
                                @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        return reports.create(me.id(), id, category, details, photos);
    }

    @PostMapping("/{id}/ratings")
    public Dto.RatingDto rate(@AuthenticationPrincipal AuthUser me, @PathVariable Long id, @Valid @RequestBody Dto.RatingReq req) {
        return ratings.rate(me.id(), id, req);
    }
}
