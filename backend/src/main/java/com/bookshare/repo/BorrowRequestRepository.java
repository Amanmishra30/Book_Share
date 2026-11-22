package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface BorrowRequestRepository extends JpaRepository<BorrowRequest, Long> {
    List<BorrowRequest> findByBorrowerIdOrderByRequestedAtDesc(Long id);
    List<BorrowRequest> findByOwnerIdOrderByRequestedAtDesc(Long id);
    boolean existsByBookIdAndStatusIn(Long bookId, Collection<BorrowStatus> statuses);
    boolean existsByBookIdAndBorrowerIdAndStatusIn(Long bookId, Long borrowerId, Collection<BorrowStatus> statuses);
    List<BorrowRequest> findByStatusAndReturnDueAtBefore(BorrowStatus status, Instant t);
    long countByOwnerIdAndStatus(Long ownerId, BorrowStatus status);
    long countByOwnerIdAndAcceptedAtIsNotNull(Long ownerId);
    long countByStatusIn(Collection<BorrowStatus> statuses);

    @Query("select count(r) from BorrowRequest r where r.borrower.id = :uid and r.returnedAt is not null and r.returnedAt <= r.returnDueAt")
    long countOnTimeReturns(@Param("uid") Long uid);

    @Query("select count(r) from BorrowRequest r where r.borrower.id = :uid and r.pickedUpAt is not null and (r.returnedAt is not null or r.returnDueAt < :now)")
    long countResolvedBorrows(@Param("uid") Long uid, @Param("now") Instant now);

    @Query("select r2.book.id, count(distinct r2.borrower.id) from BorrowRequest r1, BorrowRequest r2 " +
           "where r1.book.id = :bookId and r2.borrower.id = r1.borrower.id and r2.book.id <> :bookId " +
           "and r1.pickedUpAt is not null and r2.pickedUpAt is not null " +
           "group by r2.book.id order by count(distinct r2.borrower.id) desc")
    List<Object[]> coBorrowed(@Param("bookId") Long bookId);

    @Query("select distinct r.book.id from BorrowRequest r where r.borrower.id = :uid and r.pickedUpAt is not null")
    List<Long> borrowedBookIds(@Param("uid") Long uid);
}
