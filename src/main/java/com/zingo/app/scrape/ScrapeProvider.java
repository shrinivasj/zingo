package com.zingo.app.scrape;

public interface ScrapeProvider {
  String source();

  ScrapeResult scrape(ScrapeRequest request);
}
