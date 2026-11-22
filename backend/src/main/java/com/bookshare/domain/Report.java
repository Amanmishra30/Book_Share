package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "reports") @Getter @Setter @NoArgsConstructor
public class Report {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "borrow_request_id") private BorrowRequest borrowRequest;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "reporter_id") private User reporter;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "reported_user_id") private User reportedUser;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ReportStatus status = ReportStatus.SUBMITTED;
    @Column(length = 2000) private String details;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "report_evidence_photos", joinColumns = @JoinColumn(name = "report_id"))
    @OrderColumn(name = "photo_order")
    @Column(name = "photo_url", nullable = false)
    private List<String> evidencePhotoUrls = new ArrayList<>();
    @Column(length = 1000) private String adminNote;
    @Column(nullable = false) private Instant createdAt = Instant.now();
    private Instant resolvedAt;
}
