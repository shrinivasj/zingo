package com.zingo.app.scrape;

import java.time.LocalDate;

public record ScrapeRequest(String postalCode, String cityName, LocalDate startDate, int days) {}
