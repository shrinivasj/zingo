package com.zingo.app.controller;

import com.zingo.app.dto.ScrapeDtos.CsvImportResponse;
import com.zingo.app.dto.ScrapeDtos.ScrapeSyncRequest;
import com.zingo.app.dto.ScrapeDtos.ScrapeSyncResponse;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.scrape.CsvMovieImportResult;
import com.zingo.app.scrape.CsvMovieImportService;
import com.zingo.app.scrape.ScrapeIngestService;
import com.zingo.app.scrape.ScrapeSyncResult;
import com.zingo.app.service.AdminAccessService;
import com.zingo.app.service.AdminAuditService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping({"/api/movies", "/api/scrape"})
public class ScrapeController {
  private final ScrapeIngestService ingestService;
  private final CsvMovieImportService csvMovieImportService;
  private final AdminAccessService adminAccessService;
  private final AdminAuditService adminAuditService;

  public ScrapeController(ScrapeIngestService ingestService, CsvMovieImportService csvMovieImportService, AdminAccessService adminAccessService, AdminAuditService adminAuditService) {
    this.ingestService = ingestService;
    this.csvMovieImportService = csvMovieImportService;
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


  @PostMapping(value = "/import", consumes = "multipart/form-data")
  public CsvImportResponse importCsv(@RequestParam("file") MultipartFile file,
      @RequestParam(value = "cityId", required = false) Long cityId,
      @RequestParam(value = "postalCode", required = false) String postalCode,
      @RequestParam(value = "cityName", required = false) String cityName) {
    adminAccessService.assertCurrentUserIsOwner();
    CsvMovieImportResult result = csvMovieImportService.importCsv(file, cityId, postalCode, cityName);
    adminAuditService.logActivity("MOVIE_CSV_IMPORT", "Movie CSV imported",
        result.cityName() + " | processed " + result.rowsProcessed() + " rows, skipped " + result.rowsSkipped());
    return new CsvImportResponse(result.cityName(), result.postalCode(), com.zingo.app.entity.EventType.MOVIE,
        result.rowsProcessed(), result.rowsSkipped(), result.venuesUpserted(), result.eventsUpserted(), result.showtimesUpserted());
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
