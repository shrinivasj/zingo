package com.zingo.app.controller;

import com.zingo.app.dto.ScrapeDtos.ScrapeSyncRequest;
import com.zingo.app.dto.ScrapeDtos.ScrapeSyncResponse;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.scrape.ScrapeIngestService;
import com.zingo.app.scrape.ScrapeSyncResult;
import com.zingo.app.service.AdminAccessService;
import com.zingo.app.service.AdminAuditService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/movies", "/api/scrape"})
public class ScrapeController {
  private final ScrapeIngestService ingestService;
  private final AdminAccessService adminAccessService;
  private final AdminAuditService adminAuditService;

  public ScrapeController(ScrapeIngestService ingestService, AdminAccessService adminAccessService, AdminAuditService adminAuditService) {
    this.ingestService = ingestService;
    this.adminAccessService = adminAccessService;
    this.adminAuditService = adminAuditService;
  }

  @PostMapping("/sync")
  public ScrapeSyncResponse sync(@RequestBody ScrapeSyncRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    if (request == null || (isBlank(request.postalCode()) && isBlank(request.cityName()))) {
      throw new BadRequestException("postalCode or cityName is required");
    }
    ScrapeSyncResult result = ingestService.sync(request.postalCode(), request.cityName(), request.days());
    adminAuditService.logSync(request.cityName(), request.postalCode(), request.days(), result);
    return new ScrapeSyncResponse(result.postalCode(), result.cityName(), result.venuesUpserted(),
        result.eventsUpserted(), result.showtimesUpserted());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
