package com.zingo.app.scrape;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ScrapeHttpClient {
  private static final Logger log = LoggerFactory.getLogger(ScrapeHttpClient.class);
  private final ScrapeConfig config;
  private final HttpClient httpClient;

  public ScrapeHttpClient(ScrapeConfig config) {
    this.config = config;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public String get(String url) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("User-Agent", config.getUserAgent())
        .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
        .header("Accept-Language", "en-IN,en;q=0.9")
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        return response.body();
      }
      log.warn("Scrape HTTP {} for {}", response.statusCode(), url);
    } catch (IOException ex) {
      log.warn("Scrape HTTP error for {}", url, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("Scrape HTTP interrupted for {}", url, ex);
    }
    return "";
  }
}
