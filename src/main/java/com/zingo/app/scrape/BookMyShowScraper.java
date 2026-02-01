package com.zingo.app.scrape;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zingo.app.entity.EventType;
import com.zingo.app.entity.ShowFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class BookMyShowScraper implements ScrapeProvider {
  private final ScrapeConfig config;
  private final ScrapeHttpClient httpClient;
  private final ObjectMapper mapper;

  public BookMyShowScraper(ScrapeConfig config, ScrapeHttpClient httpClient, ObjectMapper mapper) {
    this.config = config;
    this.httpClient = httpClient;
    this.mapper = mapper;
  }

  @Override
  public String source() {
    return ScrapeSource.BOOKMYSHOW.name();
  }

  @Override
  public ScrapeResult scrape(ScrapeRequest request) {
    if (!config.getProviders().getBookmyshow().isEnabled()) {
      return ScrapeResult.empty();
    }
    String cityName = ScrapeParsing.resolveCityName(request, config);
    if (cityName == null) {
      return ScrapeResult.empty();
    }
    String citySlug = ScrapeParsing.slugify(cityName);
    if (citySlug == null) {
      return ScrapeResult.empty();
    }
    ZoneId zone = ZoneId.of(config.getZone());
    String baseUrl = config.getProviders().getBookmyshow().getBaseUrl();
    String moviesUrl = ScrapeParsing.applyTemplate(baseUrl, config.getProviders().getBookmyshow().getMoviesPathTemplate(),
        citySlug);
    String eventsUrl = ScrapeParsing.applyTemplate(baseUrl, config.getProviders().getBookmyshow().getEventsPathTemplate(),
        citySlug);

    List<ScrapedEvent> events = new ArrayList<>();
    List<ScrapedVenue> venues = new ArrayList<>();
    List<ScrapedShowtime> showtimes = new ArrayList<>();
    Set<String> seenEventIds = new HashSet<>();

    parseListingPage(moviesUrl, EventType.MOVIE, zone, events, venues, showtimes, seenEventIds);
    parseListingPage(eventsUrl, EventType.OTHER, zone, events, venues, showtimes, seenEventIds);

    CityInfo city = new CityInfo(cityName, request.postalCode(), zone.getId());
    return new ScrapeResult(city, venues, events, showtimes);
  }

  private void parseListingPage(String url, EventType defaultType, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    String html = httpClient.get(url);
    if (html.isBlank()) {
      return;
    }
    Document document = Jsoup.parse(html, url);
    List<JsonNode> jsonLdNodes = ScrapeParsing.extractJsonLd(document, mapper);
    int detailBudget = config.getMaxDetailPages();

    for (JsonNode node : jsonLdNodes) {
      if (node.has("@graph")) {
        node.get("@graph").forEach(graphNode -> extractFromNode(graphNode, defaultType, zone, events, venues, showtimes,
            seenEventIds));
      } else {
        extractFromNode(node, defaultType, zone, events, venues, showtimes, seenEventIds);
      }
    }

    if (detailBudget <= 0) {
      return;
    }

    for (Element link : document.select("a[href]")) {
      if (detailBudget <= 0) {
        break;
      }
      String href = link.attr("href");
      if (href == null || href.isBlank()) {
        continue;
      }
      if (!href.contains("/movies/") && !href.contains("/events/")) {
        continue;
      }
      String absolute = link.absUrl("href");
      if (absolute.isBlank()) {
        continue;
      }
      if (seenEventIds.contains(absolute)) {
        continue;
      }
      detailBudget--;
      parseDetailPage(absolute, defaultType, zone, events, venues, showtimes, seenEventIds);
    }
  }

  private void parseDetailPage(String url, EventType defaultType, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    String html = httpClient.get(url);
    if (html.isBlank()) {
      return;
    }
    Document document = Jsoup.parse(html, url);
    List<JsonNode> jsonLdNodes = ScrapeParsing.extractJsonLd(document, mapper);
    for (JsonNode node : jsonLdNodes) {
      if (node.has("@graph")) {
        node.get("@graph").forEach(graphNode -> extractEventNode(graphNode, defaultType, zone, events, venues, showtimes,
            seenEventIds, url));
      } else {
        extractEventNode(node, defaultType, zone, events, venues, showtimes, seenEventIds, url);
      }
    }
  }

  private void extractFromNode(JsonNode node, EventType defaultType, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds) {
    if (node.has("itemListElement")) {
      for (JsonNode itemNode : node.get("itemListElement")) {
        JsonNode item = itemNode.has("item") ? itemNode.get("item") : itemNode;
        String name = textValue(item, "name");
        String url = textValue(item, "url");
        if (name == null || url == null) {
          continue;
        }
    String sourceId = normalizeSourceId(url);
        if (!seenEventIds.add(sourceId)) {
          continue;
        }
        events.add(new ScrapedEvent(source(), sourceId, url, name, defaultType, null));
      }
    }
    extractEventNode(node, defaultType, zone, events, venues, showtimes, seenEventIds, null);
  }

  private void extractEventNode(JsonNode node, EventType defaultType, ZoneId zone, List<ScrapedEvent> events,
      List<ScrapedVenue> venues, List<ScrapedShowtime> showtimes, Set<String> seenEventIds, String fallbackUrl) {
    String type = textValue(node, "@type");
    if (type == null || !type.toLowerCase(Locale.ENGLISH).contains("event")) {
      return;
    }
    String name = textValue(node, "name");
    String url = textValue(node, "url");
    if (url == null) {
      url = fallbackUrl;
    }
    String sourceId = normalizeSourceId(url != null ? url : name);
    if (!seenEventIds.add(sourceId)) {
      return;
    }
    String image = textValue(node, "image");
    EventType resolvedType = resolveType(type, defaultType);
    events.add(new ScrapedEvent(source(), sourceId, url, name != null ? name : "Untitled", resolvedType, image));

    JsonNode location = node.get("location");
    String venueName = location != null ? textValue(location, "name") : null;
    String venueSourceId = venueName != null ? normalizeSourceId(venueName + "|" + sourceId) : null;
    if (venueName != null) {
      venues.add(new ScrapedVenue(source(), venueSourceId, url, venueName, null, null));
    }
    Instant startsAt = ScrapeParsing.parseInstant(textValue(node, "startDate"), zone);
    if (startsAt != null) {
      showtimes.add(new ScrapedShowtime(source(), sourceId + "|start", url, sourceId, venueSourceId, name,
          venueName, startsAt, ShowFormat.GENERAL));
    }
  }

  private String textValue(JsonNode node, String field) {
    if (node == null || !node.has(field)) {
      return null;
    }
    JsonNode value = node.get(field);
    if (value.isTextual()) {
      return value.asText();
    }
    if (value.isArray() && value.size() > 0 && value.get(0).isTextual()) {
      return value.get(0).asText();
    }
    return null;
  }

  private String normalizeSourceId(String raw) {
    return ScrapeParsing.normalizeSourceId(raw, 120);
  }

  private EventType resolveType(String type, EventType fallback) {
    if (type == null) {
      return fallback;
    }
    String lower = type.toLowerCase(Locale.ENGLISH);
    if (lower.contains("movie")) {
      return EventType.MOVIE;
    }
    return fallback;
  }
}
