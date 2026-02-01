package com.zingo.app.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PincodeLookupService {
  private static final Logger log = LoggerFactory.getLogger(PincodeLookupService.class);
  private final ScrapeConfig config;
  private final ObjectMapper mapper;
  private final HttpClient httpClient;

  public PincodeLookupService(ScrapeConfig config, ObjectMapper mapper) {
    this.config = config;
    this.mapper = mapper;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  public String lookupCity(String postalCode) {
    if (postalCode == null || postalCode.isBlank()) {
      return null;
    }
    if (!config.getPincodeLookup().isEnabled()) {
      return null;
    }
    String baseUrl = config.getPincodeLookup().getBaseUrl();
    String url = baseUrl.endsWith("/") ? baseUrl + postalCode.trim() : baseUrl + "/" + postalCode.trim();
    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(15))
        .header("User-Agent", config.getUserAgent())
        .header("Accept", "application/json")
        .GET()
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        log.warn("Pincode lookup HTTP {} for {}", response.statusCode(), url);
        return null;
      }
      return parseCity(response.body());
    } catch (IOException ex) {
      log.warn("Pincode lookup error for {}", url, ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("Pincode lookup interrupted for {}", url, ex);
    }
    return null;
  }

  private String parseCity(String body) {
    if (body == null || body.isBlank()) {
      return null;
    }
    try {
      JsonNode root = mapper.readTree(body);
      if (!root.isArray() || root.isEmpty()) {
        return null;
      }
      JsonNode result = root.get(0);
      if (result == null) {
        return null;
      }
      String status = textValue(result, "Status");
      if (status == null || !status.equalsIgnoreCase("Success")) {
        return null;
      }
      JsonNode postOffices = result.get("PostOffice");
      if (postOffices == null || !postOffices.isArray() || postOffices.isEmpty()) {
        return null;
      }
      JsonNode office = postOffices.get(0);
      String district = textValue(office, "District");
      if (district != null && !district.isBlank()) {
        return capitalize(district);
      }
      String region = textValue(office, "Region");
      if (region != null && !region.isBlank()) {
        return capitalize(region);
      }
      String name = textValue(office, "Name");
      if (name != null && !name.isBlank()) {
        return capitalize(name);
      }
      return null;
    } catch (Exception ex) {
      log.warn("Pincode lookup parse failed", ex);
      return null;
    }
  }

  private String textValue(JsonNode node, String field) {
    if (node == null || !node.has(field)) {
      return null;
    }
    JsonNode value = node.get(field);
    return value.isTextual() ? value.asText() : null;
  }

  private String capitalize(String value) {
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return trimmed;
    }
    String lower = trimmed.toLowerCase(Locale.ENGLISH);
    return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
  }
}
