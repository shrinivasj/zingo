package com.zingo.app.scrape;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScrapeScheduler {
  private static final Logger log = LoggerFactory.getLogger(ScrapeScheduler.class);
  private final ScrapeConfig config;
  private final ScrapeIngestService ingestService;

  public ScrapeScheduler(ScrapeConfig config, ScrapeIngestService ingestService) {
    this.config = config;
    this.ingestService = ingestService;
  }

  @Scheduled(cron = "${scrape.cron:0 0 12 * * *}", zone = "${scrape.zone:Asia/Kolkata}")
  public void refreshDaily() {
    if (!config.isEnabled()) {
      return;
    }
    List<String> pincodes = config.getPincodes();
    if (pincodes == null || pincodes.isEmpty()) {
      return;
    }
    for (String postalCode : pincodes) {
      if (postalCode == null || postalCode.isBlank()) {
        continue;
      }
      String cityName = config.getPostalCodeCityMap().get(postalCode);
      try {
        ScrapeSyncResult result = ingestService.sync(postalCode, cityName, null);
        log.info("Scrape sync {} ({}) venues={} events={} showtimes={}", result.cityName(), result.postalCode(),
            result.venuesUpserted(), result.eventsUpserted(), result.showtimesUpserted());
      } catch (Exception ex) {
        log.warn("Scrape sync failed for {}", postalCode, ex);
      }
    }
  }
}
