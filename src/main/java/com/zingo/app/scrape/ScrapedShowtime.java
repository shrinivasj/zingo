package com.zingo.app.scrape;

import com.zingo.app.entity.ShowFormat;
import java.time.Instant;

public record ScrapedShowtime(String source, String sourceId, String sourceUrl, String eventSourceId,
    String venueSourceId, String eventTitle, String venueName, Instant startsAt, ShowFormat format) {}
