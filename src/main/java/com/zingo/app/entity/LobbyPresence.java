package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "lobby_presence",
    uniqueConstraints = @UniqueConstraint(columnNames = {"showtimeId", "userId"}),
    indexes = {
      @Index(name = "idx_lobby_showtime", columnList = "showtimeId"),
      @Index(name = "idx_lobby_user", columnList = "userId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LobbyPresence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long showtimeId;

  @Column(nullable = false)
  private Long userId;

  @Column(nullable = false)
  private Instant lastSeenAt;
}
