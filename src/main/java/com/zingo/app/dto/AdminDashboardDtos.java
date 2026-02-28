package com.zingo.app.dto;

import java.time.Instant;
import java.util.List;

public class AdminDashboardDtos {
  public record AdminDashboardResponse(
      long cityCount,
      long cafePlanCount,
      long trekPlanCount,
      long movieShowtimeCount,
      List<AdminSyncRunItem> recentSyncs,
      List<AdminActivityItem> recentActivities) {}

  public record AdminSyncRunItem(
      String cityName,
      String postalCode,
      Integer daysRequested,
      int venuesUpserted,
      int eventsUpserted,
      int showtimesUpserted,
      String status,
      Instant createdAt) {}

  public record AdminActivityItem(
      String actionType,
      String title,
      String detail,
      Instant createdAt) {}
}
