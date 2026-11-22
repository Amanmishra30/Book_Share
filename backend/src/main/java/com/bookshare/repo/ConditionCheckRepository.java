package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface ConditionCheckRepository extends JpaRepository<ConditionCheck, Long> {
    List<ConditionCheck> findByBookIdOrderByCreatedAtAsc(Long bookId);
    List<ConditionCheck> findByBorrowRequestIdOrderByCreatedAtAsc(Long requestId);
    boolean existsByBorrowRequestIdAndStage(Long requestId, CheckStage stage);
}
