package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.*;
import com.bookshare.web.ApiException;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {
    private final BorrowRequestRepository reqs;
    private final ReportRepository reports;
    private final ConditionCheckService checkService;
    private final StorageService storage;
    private final Mapper mapper;

    @Value("${app.trust.grace-days}")
    private int graceDays;

    /**
     * Files a report. It never punishes anybody by itself: it joins the admin review queue with the book's
     * listing/pickup/return photos attached automatically as evidence.
     */
    @Transactional
    public Dto.ReportDto create(Long meId, Long requestId, ReportCategory category, String details, List<MultipartFile> evidence) {
        BorrowRequest r = reqs.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        boolean isOwner = r.getOwner().getId().equals(meId);
        boolean isBorrower = r.getBorrower().getId().equals(meId);
        if (!isOwner && !isBorrower) throw ApiException.forbidden("Only the two people in this exchange can report it.");
        if (reports.existsByBorrowRequestIdAndReporterIdAndCategory(requestId, meId, category)) {
            throw ApiException.conflict("You already filed this kind of report for this exchange.");
        }
        if (category == ReportCategory.NOT_RETURNED) {
            if (!isOwner) throw ApiException.forbidden("Only the owner can report a book as not returned.");
            if (r.getStatus() != BorrowStatus.RETURN_DUE || r.getReminderSentAt() == null || r.getReturnDueAt() == null) {
                throw ApiException.conflict("A reminder must be sent and the book must be overdue before it can be reported as not returned.");
            }
            Instant earliest = r.getReturnDueAt().plus(graceDays, ChronoUnit.DAYS);
            if (Instant.now().isBefore(earliest)) {
                throw ApiException.conflict("You can report a book as not returned after " + earliest + " (" + graceDays + " days grace period).");
            }
        } else if (r.getStatus() != BorrowStatus.RETURNED && r.getStatus() != BorrowStatus.COMPLETED) {
            throw ApiException.conflict("Condition reports can be filed once the book has been returned.");
        }
        List<MultipartFile> real = evidence == null ? List.of() : evidence.stream().filter(f -> !f.isEmpty()).toList();
        if (real.size() > ConditionCheckService.MAX_PHOTOS) throw ApiException.badRequest("Attach at most 5 extra photos.");
        real.forEach(StorageService::requireImage);

        Report rep = new Report();
        rep.setBorrowRequest(r);
        rep.setReporter(isOwner ? r.getOwner() : r.getBorrower());
        rep.setReportedUser(isOwner ? r.getBorrower() : r.getOwner());
        rep.setCategory(category);
        rep.setDetails(details == null || details.isBlank() ? null : details.trim());
        for (MultipartFile f : real) rep.getEvidencePhotoUrls().add(storage.store(f));
        rep = reports.save(rep);
        return mapper.report(rep, checkService.evidenceFor(r));
    }

    @Transactional(readOnly = true)
    public List<Dto.ReportDto> forRequest(Long meId, Long requestId) {
        BorrowRequest r = reqs.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (!r.getOwner().getId().equals(meId) && !r.getBorrower().getId().equals(meId)) {
            throw ApiException.forbidden("This exchange belongs to someone else.");
        }
        List<Dto.ConditionCheckDto> evidence = checkService.evidenceFor(r);
        return reports.findByBorrowRequestIdOrderByCreatedAtDesc(requestId).stream().map(x -> mapper.report(x, evidence)).toList();
    }
}
