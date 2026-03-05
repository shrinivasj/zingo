package com.zingo.app.scrape;

public record CsvMovieImportResult(
    String cityName,
    String postalCode,
    int rowsProcessed,
    int rowsSkipped,
    int venuesUpserted,
    int eventsUpserted,
    int showtimesUpserted) {}
