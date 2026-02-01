package com.zingo.app.scrape;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ScrapeOrchestrator {
  private static final Logger log = LoggerFactory.getLogger(ScrapeOrchestrator.class);
  private final List<ScrapeProvider> providers;

  public ScrapeOrchestrator(List<ScrapeProvider> providers) {
    this.providers = providers;
  }

  public List<ScrapeResult> runAll(ScrapeRequest request) {
    List<ScrapeResult> results = new ArrayList<>();
    for (ScrapeProvider provider : providers) {
      try {
        results.add(provider.scrape(request));
      } catch (Exception ex) {
        log.warn("Scrape provider {} failed", provider.source(), ex);
      }
    }
    return results;
  }
}
