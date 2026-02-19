package com.zingo.app.repository;

import com.zingo.app.entity.Message;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
  List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
  Message findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);
  @Query(
      value = """
          select ranked.id, ranked.conversation_id, ranked.sender_id, ranked.text, ranked.created_at
          from (
            select m.id, m.conversation_id, m.sender_id, m.text, m.created_at,
                   row_number() over (
                     partition by m.conversation_id
                     order by m.created_at desc, m.id desc
                   ) as rn
            from messages m
            where m.conversation_id in (:conversationIds)
          ) ranked
          where ranked.rn = 1
          """,
      nativeQuery = true)
  List<Message> findLatestByConversationIds(@Param("conversationIds") List<Long> conversationIds);
  void deleteByConversationId(Long conversationId);
}
