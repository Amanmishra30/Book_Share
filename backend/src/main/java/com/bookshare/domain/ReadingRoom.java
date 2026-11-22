package com.bookshare.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.*;

@Entity @Table(name = "reading_rooms") @Getter @Setter @NoArgsConstructor
public class ReadingRoom {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private Instant scheduledTime;
    @Column(nullable = false) private int maxParticipants = 6;
    @ManyToOne(fetch = FetchType.EAGER, optional = false) @JoinColumn(name = "host_id") private User host;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "reading_room_participants", joinColumns = @JoinColumn(name = "reading_room_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new LinkedHashSet<>();
    @Column(nullable = false) private Instant createdAt = Instant.now();
}
