package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "lobby_presence",
    uniqueConstraints = @UniqueConstraint(name = "uniq_lobby_showtime_user", columnNames = {"showtime_id", "user_id"}),
    indexes = {
      @Index(name = "idx_lobby_showtime", columnList = "showtime_id"),
      @Index(name = "idx_lobby_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class LobbyPresence {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "showtime_id", nullable = false)
  private Long showtimeId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "last_seen_at", nullable = false)
  private Instant lastSeenAt;

  @PrePersist
  public void onCreate() {
    if (lastSeenAt == null) {
      lastSeenAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getShowtimeId() {
    return showtimeId;
  }

  public void setShowtimeId(Long showtimeId) {
    this.showtimeId = showtimeId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Instant getLastSeenAt() {
    return lastSeenAt;
  }

  public void setLastSeenAt(Instant lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
  }
}
