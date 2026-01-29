package com.zingo.app.repository;

import com.zingo.app.entity.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
  List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
  Message findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);
  void deleteByConversationId(Long conversationId);
}
