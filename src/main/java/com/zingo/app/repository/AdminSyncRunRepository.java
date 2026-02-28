package com.zingo.app.repository;

import com.zingo.app.entity.AdminSyncRun;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSyncRunRepository extends JpaRepository<AdminSyncRun, Long> {
  List<AdminSyncRun> findTop8ByOrderByCreatedAtDesc();
}
