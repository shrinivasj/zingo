package com.zingo.app.service;

import com.zingo.app.dto.AdminDashboardDtos.AdminActivityItem;
import com.zingo.app.dto.AdminDashboardDtos.AdminDashboardResponse;
import com.zingo.app.dto.AdminDashboardDtos.AdminSyncRunItem;
import com.zingo.app.entity.EventType;
import com.zingo.app.repository.AdminActivityLogRepository;
import com.zingo.app.repository.AdminSyncRunRepository;
import com.zingo.app.repository.CityRepository;
import com.zingo.app.repository.ShowtimeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
  private final CityRepository cityRepository;
  private final ShowtimeRepository showtimeRepository;
  private final AdminSyncRunRepository adminSyncRunRepository;
  private final AdminActivityLogRepository adminActivityLogRepository;

  public AdminDashboardService(CityRepository cityRepository, ShowtimeRepository showtimeRepository,
      AdminSyncRunRepository adminSyncRunRepository, AdminActivityLogRepository adminActivityLogRepository) {
    this.cityRepository = cityRepository;
    this.showtimeRepository = showtimeRepository;
    this.adminSyncRunRepository = adminSyncRunRepository;
    this.adminActivityLogRepository = adminActivityLogRepository;
  }

  @Transactional(readOnly = true)
  public AdminDashboardResponse getDashboard() {
    List<AdminSyncRunItem> recentSyncs = adminSyncRunRepository.findTop8ByOrderByCreatedAtDesc().stream()
        .map(run -> new AdminSyncRunItem(
            run.getCityName(),
            run.getPostalCode(),
            run.getDaysRequested(),
            run.getVenuesUpserted() != null ? run.getVenuesUpserted() : 0,
            run.getEventsUpserted() != null ? run.getEventsUpserted() : 0,
            run.getShowtimesUpserted() != null ? run.getShowtimesUpserted() : 0,
            run.getStatus(),
            run.getCreatedAt()))
        .toList();

    List<AdminActivityItem> recentActivities = adminActivityLogRepository.findTop8ByOrderByCreatedAtDesc().stream()
        .map(log -> new AdminActivityItem(log.getActionType(), log.getTitle(), log.getDetail(), log.getCreatedAt()))
        .toList();

    return new AdminDashboardResponse(
        cityRepository.count(),
        showtimeRepository.countByAdminEventType(EventType.CAFE),
        showtimeRepository.countByAdminEventType(EventType.TREK),
        showtimeRepository.countByAdminEventType(EventType.MOVIE),
        recentSyncs,
        recentActivities);
  }
}
