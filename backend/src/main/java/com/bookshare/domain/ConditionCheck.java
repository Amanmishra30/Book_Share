package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "condition_checks") @Getter @Setter @NoArgsConstructor
public class ConditionCheck {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "book_id") private Book book;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "borrow_request_id") private BorrowRequest borrowRequest;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CheckStage stage;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "condition_check_photos", joinColumns = @JoinColumn(name = "condition_check_id"))
    @OrderColumn(name = "photo_order")
    @Column(name = "photo_url", nullable = false)
    private List<String> photoUrls = new ArrayList<>();
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "logged_by_user_id") private User loggedBy;
    @Column(length = 1000) private String aiNotes;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
