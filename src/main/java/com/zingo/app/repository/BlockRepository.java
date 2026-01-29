package com.zingo.app.repository;

import com.zingo.app.entity.Block;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockRepository extends JpaRepository<Block, Long> {
  boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
  List<Block> findByBlockerId(Long blockerId);
  List<Block> findByBlockedId(Long blockedId);
}
