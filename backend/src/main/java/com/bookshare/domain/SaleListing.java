package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "sale_listings") @Getter @Setter @NoArgsConstructor
public class SaleListing {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "book_id") private Book book;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "seller_id") private User seller;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "buyer_id") private User buyer;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private SaleStatus status = SaleStatus.LISTED;
    @Column(precision = 10, scale = 2) private BigDecimal agreedPrice;
    @Column(nullable = false) private Instant createdAt = Instant.now();

    // DEMO ONLY: records a simulated checkout; no real payment provider is contacted.
    @Column(name = "demo_payment_reference", unique = true) private String demoPaymentReference;
    @Column(name = "demo_payment_status") private String demoPaymentStatus = "NOT_STARTED";
    @Column(name = "demo_paid_at") private Instant demoPaidAt;
}
