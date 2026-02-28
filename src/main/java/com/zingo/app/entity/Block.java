package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(
    name = "blocks",
    indexes = {
      @Index(name = "idx_block_blocker", columnList = "blockerId"),
      @Index(name = "idx_block_blocked", columnList = "blockedId")
    }
)
public class Block {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long blockerId;

  @Column(nullable = false)
  private Long blockedId;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  public void onCreate() {
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBlockerId() {
    return blockerId;
  }

  public void setBlockerId(Long blockerId) {
    this.blockerId = blockerId;
  }

  public Long getBlockedId() {
    return blockedId;
  }

  public void setBlockedId(Long blockedId) {
    this.blockedId = blockedId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
