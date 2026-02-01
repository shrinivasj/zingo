package com.zingo.app.scrape;

import java.util.List;

public record ScrapeResult(CityInfo city, List<ScrapedVenue> venues, List<ScrapedEvent> events,
    List<ScrapedShowtime> showtimes) {
  public static ScrapeResult empty() {
    return new ScrapeResult(null, List.of(), List.of(), List.of());
  }
}
