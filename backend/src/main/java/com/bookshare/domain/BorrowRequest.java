package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "borrow_requests") @Getter @Setter @NoArgsConstructor
public class BorrowRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "book_id") private Book book;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "borrower_id") private User borrower;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owner_id") private User owner;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private BorrowStatus status = BorrowStatus.PENDING;
    @Column(length = 500) private String note;
    @Column(nullable = false) private Instant requestedAt = Instant.now();
    private Instant acceptedAt;
    private Instant pickedUpAt;
    private Instant returnDueAt;
    private Instant returnedAt;
    private Instant completedAt;
    private Instant reminderSentAt;
}
