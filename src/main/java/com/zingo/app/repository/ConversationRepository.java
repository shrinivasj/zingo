package com.zingo.app.repository;

import com.zingo.app.entity.Conversation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
  @Query(
      "select c from Conversation c where c.id in (select m1.conversationId from ConversationMember m1 where m1.userId = :userA) "
          + "and c.id in (select m2.conversationId from ConversationMember m2 where m2.userId = :userB) "
          + "order by c.createdAt desc, c.id desc")
  List<Conversation> findExistingByUsers(@Param("userA") Long userA, @Param("userB") Long userB);

  @Query(
      "select c from Conversation c where c.id in (select m.conversationId from ConversationMember m where m.userId = :userId)")
  List<Conversation> findByMember(@Param("userId") Long userId);

  List<Conversation> findByShowtimeId(Long showtimeId);
}
