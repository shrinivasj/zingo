package com.zingo.app.repository;

import com.zingo.app.entity.TrekJoinRequest;
import com.zingo.app.entity.TrekJoinRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrekJoinRequestRepository extends JpaRepository<TrekJoinRequest, Long> {
  boolean existsByGroupIdAndRequesterUserIdAndStatus(Long groupId, Long requesterUserId, TrekJoinRequestStatus status);
  Optional<TrekJoinRequest> findFirstByGroupIdAndRequesterUserIdAndStatusOrderByUpdatedAtDesc(
      Long groupId, Long requesterUserId, TrekJoinRequestStatus status);
  long countByGroupIdAndStatus(Long groupId, TrekJoinRequestStatus status);
  Optional<TrekJoinRequest> findByIdAndStatus(Long id, TrekJoinRequestStatus status);
  List<TrekJoinRequest> findByGroupIdInAndStatusOrderByCreatedAtDesc(List<Long> groupIds, TrekJoinRequestStatus status);
  void deleteByGroupIdIn(List<Long> groupIds);
}
