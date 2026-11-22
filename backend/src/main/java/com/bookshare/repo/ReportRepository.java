package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByBorrowRequestIdOrderByCreatedAtDesc(Long requestId);
    List<Report> findByStatusInOrderByCreatedAtAsc(Collection<ReportStatus> statuses);
    List<Report> findAllByOrderByCreatedAtDesc();
    long countByReportedUserIdAndStatus(Long userId, ReportStatus status);
    long countByReportedUserId(Long userId);
    long countByStatusIn(Collection<ReportStatus> statuses);
    boolean existsByBorrowRequestIdAndReporterIdAndCategory(Long requestId, Long reporterId, ReportCategory c);

    @Query("select r.reportedUser.id, count(r) from Report r where r.status = com.bookshare.domain.ReportStatus.VALID group by r.reportedUser.id")
    List<Object[]> validCountsByUser();
}
