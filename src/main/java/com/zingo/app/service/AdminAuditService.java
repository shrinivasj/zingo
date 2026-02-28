package com.zingo.app.service;

import com.zingo.app.entity.AdminActivityLog;
import com.zingo.app.entity.AdminSyncRun;
import com.zingo.app.scrape.ScrapeSyncResult;
import com.zingo.app.security.SecurityUtil;
import com.zingo.app.repository.AdminActivityLogRepository;
import com.zingo.app.repository.AdminSyncRunRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {
  private final AdminActivityLogRepository adminActivityLogRepository;
  private final AdminSyncRunRepository adminSyncRunRepository;

  public AdminAuditService(AdminActivityLogRepository adminActivityLogRepository,
      AdminSyncRunRepository adminSyncRunRepository) {
    this.adminActivityLogRepository = adminActivityLogRepository;
    this.adminSyncRunRepository = adminSyncRunRepository;
  }

  @Transactional
  public void logActivity(String actionType, String title, String detail) {
    AdminActivityLog log = new AdminActivityLog();
    log.setActorUserId(SecurityUtil.currentUserId());
    log.setActionType(actionType);
    log.setTitle(title);
    log.setDetail(detail);
    log.setCreatedAt(Instant.now());
    adminActivityLogRepository.save(log);
  }

  @Transactional
  public void logSync(String cityName, String postalCode, Integer daysRequested, ScrapeSyncResult result) {
    AdminSyncRun syncRun = new AdminSyncRun();
    syncRun.setActorUserId(SecurityUtil.currentUserId());
    syncRun.setCityName(blankToNull(result.cityName() != null ? result.cityName() : cityName));
    syncRun.setPostalCode(blankToNull(result.postalCode() != null ? result.postalCode() : postalCode));
    syncRun.setDaysRequested(daysRequested);
    syncRun.setVenuesUpserted(result.venuesUpserted());
    syncRun.setEventsUpserted(result.eventsUpserted());
    syncRun.setShowtimesUpserted(result.showtimesUpserted());
    syncRun.setStatus(result.eventsUpserted() + result.venuesUpserted() + result.showtimesUpserted() > 0 ? "SUCCESS" : "NO_DATA");
    syncRun.setCreatedAt(Instant.now());
    adminSyncRunRepository.save(syncRun);

    logActivity(
        "MOVIE_SYNC",
        "Movie sync completed",
        buildSyncDetail(syncRun));
  }

  private String buildSyncDetail(AdminSyncRun syncRun) {
    String location = syncRun.getCityName() != null ? syncRun.getCityName() : "Selected location";
    if (syncRun.getPostalCode() != null) {
      location = location + " · " + syncRun.getPostalCode();
    }
    return location + " | " + syncRun.getEventsUpserted() + " events, " + syncRun.getVenuesUpserted()
        + " venues, " + syncRun.getShowtimesUpserted() + " showtimes";
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
