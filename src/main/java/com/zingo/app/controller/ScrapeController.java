package com.zingo.app.controller;

import com.zingo.app.dto.ScrapeDtos.ScrapeSyncRequest;
import com.zingo.app.dto.ScrapeDtos.ScrapeSyncResponse;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.scrape.ScrapeIngestService;
import com.zingo.app.scrape.ScrapeSyncResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scrape")
public class ScrapeController {
  private final ScrapeIngestService ingestService;

  public ScrapeController(ScrapeIngestService ingestService) {
    this.ingestService = ingestService;
  }

  @PostMapping("/sync")
  public ScrapeSyncResponse sync(@RequestBody ScrapeSyncRequest request) {
    if (request == null || (isBlank(request.postalCode()) && isBlank(request.cityName()))) {
      throw new BadRequestException("postalCode or cityName is required");
    }
    ScrapeSyncResult result = ingestService.sync(request.postalCode(), request.cityName(), request.days());
    return new ScrapeSyncResponse(result.postalCode(), result.cityName(), result.venuesUpserted(),
        result.eventsUpserted(), result.showtimesUpserted());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
