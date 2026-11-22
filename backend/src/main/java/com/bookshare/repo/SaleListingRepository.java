package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface SaleListingRepository extends JpaRepository<SaleListing, Long> {
    List<SaleListing> findByBookIdInAndStatusIn(Collection<Long> bookIds, Collection<SaleStatus> statuses);
    Optional<SaleListing> findFirstByBookIdAndStatusIn(Long bookId, Collection<SaleStatus> statuses);
    List<SaleListing> findByStatusInOrderByCreatedAtDesc(Collection<SaleStatus> statuses);
    List<SaleListing> findBySellerIdOrderByCreatedAtDesc(Long sellerId);
    long countByStatusIn(Collection<SaleStatus> statuses);
}
