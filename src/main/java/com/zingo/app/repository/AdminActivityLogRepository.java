package com.zingo.app.repository;

import com.zingo.app.entity.AdminActivityLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogRepository extends JpaRepository<AdminActivityLog, Long> {
  List<AdminActivityLog> findTop8ByOrderByCreatedAtDesc();
}
