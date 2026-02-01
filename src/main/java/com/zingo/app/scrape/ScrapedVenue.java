package com.zingo.app.scrape;

public record ScrapedVenue(String source, String sourceId, String sourceUrl, String name, String address,
    String postalCode) {}
