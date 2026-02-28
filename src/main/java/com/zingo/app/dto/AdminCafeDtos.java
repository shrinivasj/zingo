package com.zingo.app.dto;

import com.zingo.app.entity.EventType;
import java.time.Instant;
import java.util.List;

public class AdminCafeDtos {
  public record CreateCafePlanRequest(
      Long cityId,
      String venueName,
      String title,
      Instant startsAt,
      String address,
      String postalCode) {}

  public record CreateCafePlanResponse(
      Long cityId,
      String cityName,
      Long venueId,
      String venueName,
      Long eventId,
      String title,
      Long showtimeId,
      Instant startsAt,
      String address,
      String postalCode,
      EventType type,
      boolean venueCreated,
      boolean eventCreated,
      boolean showtimeCreated) {}

  public record CreateTrekPlanRequest(
      Long cityId,
      String venueName,
      String title,
      Instant startsAt,
      String address,
      String postalCode) {}

  public record AdminPlanListResponse(List<CreateCafePlanResponse> plans) {}
}
