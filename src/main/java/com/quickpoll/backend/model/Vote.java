package com.quickpoll.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "votes", uniqueConstraints = @UniqueConstraint(columnNames = { "poll_id", "voter_token" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "poll_id")
    private Poll poll;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id")
    private PollOption option;

    @Column(name = "voter_token", nullable = false)
    private String voterToken;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
