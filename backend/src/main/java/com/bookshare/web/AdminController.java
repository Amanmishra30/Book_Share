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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService admin;

    @GetMapping("/stats")
    public Dto.StatsDto stats() { return admin.stats(); }

    /** status = OPEN (default) | ALL | SUBMITTED | UNDER_REVIEW | VALID | INVALID */
    @GetMapping("/reports")
    public List<Dto.ReportDto> reports(@RequestParam(defaultValue = "OPEN") String status) { return admin.reports(status); }

    @PutMapping("/reports/{id}")
    public Dto.ReportDto update(@PathVariable Long id, @Valid @RequestBody Dto.AdminReportReq req) { return admin.updateReport(id, req); }

    @GetMapping("/flagged-users")
    public List<Dto.FlaggedUserDto> flagged() { return admin.flaggedUsers(); }

    @PutMapping("/users/{id}/status")
    public Dto.UserDto setStatus(@PathVariable Long id, @Valid @RequestBody Dto.StatusReq req) { return admin.setUserStatus(id, req.status()); }

    @GetMapping("/sales")
    public List<Dto.SaleDto> sales() { return admin.activeSales(); }
}
