package com.zingo.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PushDeliveryService {
  private static final Logger log = LoggerFactory.getLogger(PushDeliveryService.class);
  private static final String FCM_ENDPOINT = "https://fcm.googleapis.com/fcm/send";

  private final PushTokenService pushTokenService;
  private final ObjectMapper objectMapper;
  private final String fcmServerKey;
  private final HttpClient httpClient;

  public PushDeliveryService(
      PushTokenService pushTokenService,
      ObjectMapper objectMapper,
      @Value("${app.push.fcmServerKey:}") String fcmServerKey) {
    this.pushTokenService = pushTokenService;
    this.objectMapper = objectMapper;
    this.fcmServerKey = fcmServerKey == null ? "" : fcmServerKey.trim();
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  public void sendToUser(Long userId, String title, String body, Map<String, Object> data) {
    if (fcmServerKey.isBlank()) {
      return;
    }
    for (String token : pushTokenService.listTokensForUser(userId)) {
      sendToToken(token, title, body, data);
    }
  }

  private void sendToToken(String token, String title, String body, Map<String, Object> data) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("to", token);
    payload.put("priority", "high");

    Map<String, String> notification = new LinkedHashMap<>();
    notification.put("title", title == null ? "Aurofly" : title);
    notification.put("body", body == null ? "" : body);
    payload.put("notification", notification);
    payload.put("data", stringifyData(data));

    String bodyText;
    try {
      bodyText = objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException exception) {
      return;
    }

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(FCM_ENDPOINT))
        .timeout(Duration.ofSeconds(8))
        .header("Content-Type", "application/json")
        .header("Authorization", "key=" + fcmServerKey)
        .POST(HttpRequest.BodyPublishers.ofString(bodyText))
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        log.warn("FCM push failed: status={} body={}", response.statusCode(), response.body());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      log.warn("FCM push request failed", exception);
    } catch (IOException exception) {
      log.warn("FCM push request failed", exception);
    }
  }

  private Map<String, String> stringifyData(Map<String, Object> data) {
    Map<String, String> result = new LinkedHashMap<>();
    if (data == null) {
      return result;
    }
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      result.put(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
    }
    return result;
  }
}
