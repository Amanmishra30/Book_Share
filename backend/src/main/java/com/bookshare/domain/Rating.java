package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "ratings", uniqueConstraints = @UniqueConstraint(columnNames = {"borrow_request_id", "rater_id"}))
@Getter @Setter @NoArgsConstructor
public class Rating {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "borrow_request_id") private BorrowRequest borrowRequest;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "rater_id") private User rater;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "ratee_id") private User ratee;
    private int communication;
    private int care;
    private int reliability;
    @Column(length = 1000) private String comment;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
