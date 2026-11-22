package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface RatingRepository extends JpaRepository<Rating, Long> {
    List<Rating> findByRateeIdOrderByCreatedAtDesc(Long rateeId);
    List<Rating> findByBorrowRequestId(Long requestId);
    boolean existsByBorrowRequestIdAndRaterId(Long requestId, Long raterId);

    @Query("select avg(r.communication), avg(r.care), avg(r.reliability), count(r) from Rating r where r.ratee.id = :uid")
    List<Object[]> averages(@Param("uid") Long uid);
}
