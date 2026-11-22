package com.bookshare.service;

import com.bookshare.domain.BorrowStatus;
import com.bookshare.repo.BorrowRequestRepository;
import com.bookshare.repo.RatingRepository;
import com.bookshare.repo.UserRepository;
import com.bookshare.web.Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Two separate numbers per user:
 *  - star rating: average of the communication / care / reliability ratings received
 *  - compliance: on-time returns / resolved borrows, and completed lends / accepted lends,
 *    always computed from the immutable borrow history (never from a user-editable field).
 * "Resolved borrows" = picked-up books that were returned or are already past their due date.
 */
@Service
@RequiredArgsConstructor
public class ReputationService {
    private final BorrowRequestRepository reqs;
    private final RatingRepository ratings;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Dto.ComplianceDto compliance(Long uid) {
        long onTime = reqs.countOnTimeReturns(uid);
        long total = reqs.countResolvedBorrows(uid, Instant.now());
        long completed = reqs.countByOwnerIdAndStatus(uid, BorrowStatus.COMPLETED);
        long accepted = reqs.countByOwnerIdAndAcceptedAtIsNotNull(uid);
        return new Dto.ComplianceDto(onTime, total, total == 0 ? null : (double) onTime / total,
                completed, accepted, accepted == 0 ? null : (double) completed / accepted);
    }

    /** @return {avgCommunication, avgCare, avgReliability, count} */
    @Transactional(readOnly = true)
    public double[] averages(Long uid) {
        List<Object[]> rows = ratings.averages(uid);
        Object[] r = rows.isEmpty() ? new Object[]{null, null, null, 0L} : rows.get(0);
        return new double[]{num(r[0]), num(r[1]), num(r[2]), num(r[3])};
    }

    @Transactional
    public void recalc(Long uid) {
        users.findById(uid).ifPresent(u -> {
            double[] a = averages(uid);
            u.setReputationScore(a[3] == 0 ? 0 : (a[0] + a[1] + a[2]) / 3.0);
            Dto.ComplianceDto c = compliance(uid);
            u.setComplianceRatio(c.onTimeRatio() == null ? 0 : c.onTimeRatio());
            users.save(u);
        });
    }

    private static double num(Object o) { return o == null ? 0 : ((Number) o).doubleValue(); }
}
