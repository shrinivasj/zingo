package com.zingo.app.dto;

import com.zingo.app.entity.EventType;

public final class ScrapeDtos {
  private ScrapeDtos() {}

  public record ScrapeSyncRequest(String postalCode, String cityName, Integer days) {}

  public record ScrapeSyncResponse(String postalCode, String cityName, int venuesUpserted, int eventsUpserted,
      int showtimesUpserted) {}

  public record CsvImportResponse(String cityName, String postalCode, EventType eventType, int rowsProcessed,
      int rowsSkipped, int venuesUpserted, int eventsUpserted, int showtimesUpserted) {}
}
