package com.bookshare.repo;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.bookshare.domain.*;
import java.time.Instant;
import java.util.*;

public interface ReadingRoomRepository extends JpaRepository<ReadingRoom, Long> {
    List<ReadingRoom> findAllByOrderByScheduledTimeAsc();
}
