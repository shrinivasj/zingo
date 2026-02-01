package com.zingo.app.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class ScrapeParsing {
  private static final DateTimeFormatter HUMAN_DATE_TIME = new DateTimeFormatterBuilder()
      .parseCaseInsensitive()
      .appendPattern("EEE, d MMM")
      .optionalStart()
      .appendLiteral(',')
      .optionalEnd()
      .optionalStart()
      .appendPattern(" yyyy")
      .optionalEnd()
      .optionalStart()
      .appendLiteral(',')
      .optionalEnd()
      .optionalStart()
      .appendPattern(" h:mm a")
      .optionalEnd()
      .parseDefaulting(ChronoField.YEAR, Year.now().getValue())
      .parseDefaulting(ChronoField.HOUR_OF_DAY, 19)
      .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
      .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
      .toFormatter(Locale.ENGLISH);

  private ScrapeParsing() {}

  public static String slugify(String input) {
    if (input == null) {
      return null;
    }
    String normalized = input.trim().toLowerCase(Locale.ENGLISH);
    if (normalized.isEmpty()) {
      return null;
    }
    return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
  }

  public static String resolveCityName(ScrapeRequest request, ScrapeConfig config) {
    if (request.cityName() != null && !request.cityName().isBlank()) {
      return request.cityName().trim();
    }
    if (request.postalCode() == null || request.postalCode().isBlank()) {
      return null;
    }
    String mapped = config.getPostalCodeCityMap().get(request.postalCode());
    if (mapped != null && !mapped.isBlank()) {
      return mapped.trim();
    }
    return null;
  }

  public static String applyTemplate(String baseUrl, String template, String citySlug) {
    String path = template.replace("{citySlug}", citySlug);
    if (path.startsWith("http")) {
      return path;
    }
    if (baseUrl.endsWith("/") && path.startsWith("/")) {
      return baseUrl.substring(0, baseUrl.length() - 1) + path;
    }
    if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
      return baseUrl + "/" + path;
    }
    return baseUrl + path;
  }

  public static List<JsonNode> extractJsonLd(Document document, ObjectMapper mapper) {
    List<JsonNode> nodes = new ArrayList<>();
    Elements scripts = document.select("script[type=application/ld+json]");
    for (Element script : scripts) {
      String json = script.data();
      if (json == null || json.isBlank()) {
        json = script.html();
      }
      if (json == null || json.isBlank()) {
        continue;
      }
      try {
        JsonNode node = mapper.readTree(json);
        if (node.isArray()) {
          node.forEach(nodes::add);
        } else {
          nodes.add(node);
        }
      } catch (Exception ignored) {
        // Ignore malformed JSON-LD blocks.
      }
    }
    return nodes;
  }

  public static String normalizeSourceId(String raw, int maxLen) {
    if (raw == null) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    if (trimmed.length() <= maxLen) {
      return trimmed;
    }
    return sha256Hex(trimmed);
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(bytes.length * 2);
      for (byte b : bytes) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      return Integer.toHexString(value.hashCode());
    }
  }

  public static Instant parseInstant(String value, ZoneId defaultZone) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(value);
      return localDateTime.atZone(defaultZone).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      LocalDate date = LocalDate.parse(value);
      return date.atStartOfDay(defaultZone).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      LocalDateTime localDateTime = LocalDateTime.parse(value, HUMAN_DATE_TIME);
      return localDateTime.atZone(defaultZone).toInstant();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    return null;
  }
}
