package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "communities") @Getter @Setter @NoArgsConstructor
public class Community {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CommunityLevel level;
    @ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "parent_community_id") private Community parent;
}
