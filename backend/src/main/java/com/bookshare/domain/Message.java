package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "messages") @Getter @Setter @NoArgsConstructor
public class Message {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "borrow_request_id") private BorrowRequest borrowRequest;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "sale_listing_id") private SaleListing saleListing;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id") private Room room;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reading_room_id") private ReadingRoom readingRoom;
    /** For sale-listing chats: the interested buyer who owns the thread (one thread per buyer). */
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "thread_user_id") private User threadUser;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "sender_id") private User sender;
    @Column(nullable = false, length = 2000) private String content;
    @Column(nullable = false) private boolean systemMessage = false;
    @Column(nullable = false) private Instant sentAt = Instant.now();
}
