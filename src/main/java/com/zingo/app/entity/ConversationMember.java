package com.zingo.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "conversation_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"conversationId", "userId"}),
    indexes = {
      @Index(name = "idx_conv_member_user", columnList = "userId")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ConversationMember {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long conversationId;

  @Column(nullable = false)
  private Long userId;
}
