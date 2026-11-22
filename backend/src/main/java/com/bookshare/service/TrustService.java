package com.bookshare.service;

import com.bookshare.domain.*;
import com.bookshare.repo.ReportRepository;
import com.bookshare.repo.UserRepository;
import com.bookshare.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Escalation ladder. A report never punishes anyone by itself: only reports an admin has marked VALID count.
 * 1 valid -> WARNED, 2 valid -> RESTRICTED (cannot borrow, can still lend), 3 or more -> UNDER_REVIEW.
 */
@Service
@RequiredArgsConstructor
public class TrustService {
    private final UserRepository users;
    private final ReportRepository reports;

    @Value("${app.trust.restriction-days}")
    private int restrictionDays;

    public void assertCanBorrow(User u) {
        switch (u.getAccountStatus()) {
            case SUSPENDED -> throw ApiException.forbidden("Your account is suspended.");
            case UNDER_REVIEW -> throw ApiException.forbidden("Your account is under admin review, so borrowing is paused.");
            case RESTRICTED -> {
                if (u.getRestrictedUntil() == null || u.getRestrictedUntil().isAfter(Instant.now())) {
                    throw ApiException.forbidden("Borrowing is paused on your account"
                            + (u.getRestrictedUntil() == null ? "." : " until " + u.getRestrictedUntil() + "."));
                }
            }
            default -> { }
        }
    }

    public void assertCanLend(User u) {
        if (u.getAccountStatus() == AccountStatus.SUSPENDED || u.getAccountStatus() == AccountStatus.UNDER_REVIEW) {
            throw ApiException.forbidden("Your account is " + (u.getAccountStatus() == AccountStatus.SUSPENDED
                    ? "suspended." : "under admin review, so lending is paused."));
        }
    }

    @Transactional
    public void escalate(User u) {
        if (u.getAccountStatus() == AccountStatus.SUSPENDED) return;
        long valid = reports.countByReportedUserIdAndStatus(u.getId(), ReportStatus.VALID);
        if (valid == 0) {
            u.setAccountStatus(AccountStatus.ACTIVE);
            u.setRestrictedUntil(null);
        } else if (valid == 1) {
            u.setAccountStatus(AccountStatus.WARNED);
            u.setRestrictedUntil(null);
        } else if (valid == 2) {
            u.setAccountStatus(AccountStatus.RESTRICTED);
            u.setRestrictedUntil(Instant.now().plus(restrictionDays, ChronoUnit.DAYS));
        } else {
            u.setAccountStatus(AccountStatus.UNDER_REVIEW);
            u.setRestrictedUntil(null);
        }
        users.save(u);
    }
}
