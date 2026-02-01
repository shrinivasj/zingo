package com.zingo.app.scrape;

import com.zingo.app.entity.EventType;

public record ScrapedEvent(String source, String sourceId, String sourceUrl, String title, EventType type,
    String posterUrl) {}
