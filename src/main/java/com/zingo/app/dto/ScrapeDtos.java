package com.zingo.app.dto;

public final class ScrapeDtos {
  private ScrapeDtos() {}

  public record ScrapeSyncRequest(String postalCode, String cityName, Integer days) {}

  public record ScrapeSyncResponse(String postalCode, String cityName, int venuesUpserted, int eventsUpserted,
      int showtimesUpserted) {}
}
