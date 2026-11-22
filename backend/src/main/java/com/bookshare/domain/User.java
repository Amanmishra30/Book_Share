package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "users") @Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "community_id") private Community community;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Role role = Role.USER;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AccountStatus accountStatus = AccountStatus.ACTIVE;
    private Instant restrictedUntil;
    private double reputationScore;
    private double complianceRatio;
    @Column(length = 500) private String bio;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
