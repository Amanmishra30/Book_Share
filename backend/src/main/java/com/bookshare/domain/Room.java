package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "rooms") @Getter @Setter @NoArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    private String topic;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RoomType type = RoomType.CHAT;
    private Instant scheduledAt;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "created_by") private User createdBy;
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
