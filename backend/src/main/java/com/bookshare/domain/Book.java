package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "books") @Getter @Setter @NoArgsConstructor
public class Book {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "owner_id") private User owner;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "community_id") private Community community;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String author;
    private String genre;
    private String edition;
    @Enumerated(EnumType.STRING) @Column(name = "book_condition", nullable = false) private BookCondition bookCondition = BookCondition.GOOD;
    private String coverUrl;
    @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ListingType listingType = ListingType.BORROW;
    @Column(precision = 10, scale = 2) private BigDecimal price;
    @Column(nullable = false) private int lendingDurationDays = 14;
    @Column(nullable = false) private boolean available = true;
    @Column(nullable = false) private boolean archived = false;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
