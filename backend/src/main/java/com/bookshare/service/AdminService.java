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

@Service
@RequiredArgsConstructor
public class AdminService {
    private final ReportRepository reports;
    private final UserRepository users;
    private final BookRepository books;
    private final BorrowRequestRepository reqs;
    private final SaleListingRepository sales;
    private final ConditionCheckService checks;
    private final TrustService trust;
    private final Mapper mapper;

    @Value("${app.trust.restriction-days}")
    private int restrictionDays;

    @Transactional(readOnly = true)
    public List<Dto.ReportDto> reports(String status) {
        List<Report> list;
        if (status == null || status.isBlank() || status.equalsIgnoreCase("ALL")) list = reports.findAllByOrderByCreatedAtDesc();
        else if (status.equalsIgnoreCase("OPEN")) list = reports.findByStatusInOrderByCreatedAtAsc(List.of(ReportStatus.SUBMITTED, ReportStatus.UNDER_REVIEW));
        else list = reports.findByStatusInOrderByCreatedAtAsc(List.of(ReportStatus.valueOf(status.toUpperCase())));
        return list.stream().map(r -> mapper.report(r, checks.evidenceFor(r.getBorrowRequest()))).toList();
    }

    /** Marks a report UNDER_REVIEW / VALID / INVALID, then re-evaluates the reported user's escalation level. */
    @Transactional
    public Dto.ReportDto updateReport(Long id, Dto.AdminReportReq req) {
        Report r = reports.findById(id).orElseThrow(() -> ApiException.notFound("Report not found"));
        if (req.status() == ReportStatus.SUBMITTED) throw ApiException.badRequest("Choose UNDER_REVIEW, VALID or INVALID.");
        r.setStatus(req.status());
        r.setAdminNote(req.adminNote() == null || req.adminNote().isBlank() ? null : req.adminNote().trim());
        r.setResolvedAt(req.status() == ReportStatus.UNDER_REVIEW ? null : Instant.now());
        reports.save(r);
        trust.escalate(r.getReportedUser());
        return mapper.report(r, checks.evidenceFor(r.getBorrowRequest()));
    }

    @Transactional(readOnly = true)
    public List<Dto.FlaggedUserDto> flaggedUsers() {
        Map<Long, Long> valid = new HashMap<>();
        for (Object[] row : reports.validCountsByUser()) valid.put((Long) row[0], ((Number) row[1]).longValue());
        Set<Long> ids = new LinkedHashSet<>(valid.keySet());
        users.findByAccountStatusNot(AccountStatus.ACTIVE).forEach(u -> ids.add(u.getId()));
        return users.findAllById(ids).stream()
                .map(u -> new Dto.FlaggedUserDto(mapper.user(u), u.getAccountStatus().name(), valid.getOrDefault(u.getId(), 0L),
                        reports.countByReportedUserId(u.getId()), u.getRestrictedUntil()))
                .sorted(Comparator.comparingLong(Dto.FlaggedUserDto::validReports).reversed())
                .toList();
    }

    @Transactional
    public Dto.UserDto setUserStatus(Long userId, AccountStatus status) {
        User u = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        if (u.getRole() == Role.ADMIN) throw ApiException.badRequest("Admin accounts cannot be restricted.");
        u.setAccountStatus(status);
        u.setRestrictedUntil(status == AccountStatus.RESTRICTED ? Instant.now().plus(restrictionDays, ChronoUnit.DAYS) : null);
        return mapper.userDto(users.save(u));
    }

    @Transactional(readOnly = true)
    public List<Dto.SaleDto> activeSales() {
        return sales.findByStatusInOrderByCreatedAtDesc(SaleService.ACTIVE).stream().map(mapper::sale).toList();
    }

    @Transactional(readOnly = true)
    public Dto.StatsDto stats() {
        return new Dto.StatsDto(users.count(), books.countByArchivedFalse(),
                reqs.countByStatusIn(List.of(BorrowStatus.PENDING, BorrowStatus.ACCEPTED, BorrowStatus.PICKED_UP, BorrowStatus.BORROWED, BorrowStatus.RETURN_DUE)),
                reports.countByStatusIn(List.of(ReportStatus.SUBMITTED, ReportStatus.UNDER_REVIEW)),
                sales.countByStatusIn(SaleService.ACTIVE));
    }
}
