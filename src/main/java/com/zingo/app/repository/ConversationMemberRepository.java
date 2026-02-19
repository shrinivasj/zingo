package com.zingo.app.repository;

import com.zingo.app.entity.ConversationMember;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {
  List<ConversationMember> findByConversationId(Long conversationId);
  List<ConversationMember> findByConversationIdIn(List<Long> conversationIds);
  boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
  long countByConversationId(Long conversationId);
  void deleteByConversationIdAndUserId(Long conversationId, Long userId);
}
