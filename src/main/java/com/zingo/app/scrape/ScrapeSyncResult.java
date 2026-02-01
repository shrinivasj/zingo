package com.zingo.app.scrape;

public record ScrapeSyncResult(String postalCode, String cityName, int venuesUpserted, int eventsUpserted,
    int showtimesUpserted) {}
