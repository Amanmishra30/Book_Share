package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByBorrowRequestIdOrderBySentAtAsc(Long id);
    List<Message> findBySaleListingIdAndThreadUserIdOrderBySentAtAsc(Long saleId, Long threadUserId);
    List<Message> findByRoomIdOrderBySentAtAsc(Long id);
    List<Message> findByReadingRoomIdOrderBySentAtAsc(Long id);
    void deleteByReadingRoomId(Long id);
    boolean existsBySaleListingIdAndThreadUserId(Long saleId, Long threadUserId);

    @Query("select distinct m.threadUser.id from Message m where m.saleListing.id = :saleId and m.threadUser is not null")
    List<Long> threadUserIds(@Param("saleId") Long saleId);
}
