package com.zingo.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Service;

@Service
public class AdminConfigService {
  private static final String OVERRIDES_SOURCE_NAME = "adminConfigOverrides";

  private final ConfigurableEnvironment environment;
  private final ObjectMapper objectMapper;
  private final Path overridesPath;
  private final Map<String, Object> overrides = new LinkedHashMap<>();

  public AdminConfigService(
      ConfigurableEnvironment environment,
      ObjectMapper objectMapper,
      @Value("${app.admin.overridesFile:config/application-admin-overrides.properties}") String overridesFile) {
    this.environment = environment;
    this.objectMapper = objectMapper;
    this.overridesPath = Paths.get(overridesFile).toAbsolutePath().normalize();
  }

  @PostConstruct
  public void initialize() {
    loadOverridesFromDisk();
    registerOrReplacePropertySource();
  }

  public synchronized void putOverride(String key, String valueText) {
    Object value = parseValueText(valueText);
    overrides.put(key, value);
    registerOrReplacePropertySource();
    persistOverridesToDisk();
  }

  private void registerOrReplacePropertySource() {
    if (environment.getPropertySources().contains(OVERRIDES_SOURCE_NAME)) {
      environment.getPropertySources().replace(OVERRIDES_SOURCE_NAME,
          new MapPropertySource(OVERRIDES_SOURCE_NAME, new LinkedHashMap<>(overrides)));
      return;
    }
    environment.getPropertySources().addFirst(
        new MapPropertySource(OVERRIDES_SOURCE_NAME, new LinkedHashMap<>(overrides)));
  }

  private void loadOverridesFromDisk() {
    if (!Files.exists(overridesPath)) {
      return;
    }
    Properties properties = new Properties();
    try (InputStream inputStream = Files.newInputStream(overridesPath)) {
      properties.load(inputStream);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to read admin overrides file: " + overridesPath, exception);
    }

    for (String name : properties.stringPropertyNames()) {
      String text = properties.getProperty(name);
      overrides.put(name, parseValueText(text));
    }
  }

  private void persistOverridesToDisk() {
    Path parent = overridesPath.getParent();
    if (parent == null) {
      parent = Paths.get(".").toAbsolutePath().normalize();
    }
    try {
      Files.createDirectories(parent);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to create admin overrides directory", exception);
    }

    Properties properties = new Properties();
    for (Map.Entry<String, Object> entry : overrides.entrySet()) {
      properties.setProperty(entry.getKey(), stringifyForStorage(entry.getValue()));
    }

    try (OutputStream outputStream = Files.newOutputStream(overridesPath)) {
      properties.store(outputStream, "Owner-managed admin config overrides");
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to write admin overrides file: " + overridesPath, exception);
    }
  }

  private Object parseValueText(String text) {
    if (text == null) {
      return "";
    }
    String trimmed = text.trim();
    if (trimmed.isEmpty()) {
      return "";
    }

    if ("true".equalsIgnoreCase(trimmed)) {
      return Boolean.TRUE;
    }
    if ("false".equalsIgnoreCase(trimmed)) {
      return Boolean.FALSE;
    }

    try {
      if (trimmed.matches("^-?\\d+$")) {
        return Long.parseLong(trimmed);
      }
      if (trimmed.matches("^-?\\d+\\.\\d+$")) {
        return Double.parseDouble(trimmed);
      }
    } catch (NumberFormatException ignored) {
      // Fall through to other parsers.
    }

    if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
        || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
      try {
        return objectMapper.readValue(trimmed, Object.class);
      } catch (JsonProcessingException ignored) {
        // Keep as plain text if JSON is invalid.
      }
    }

    return text;
  }

  private String stringifyForStorage(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof String || value instanceof Number || value instanceof Boolean) {
      return String.valueOf(value);
    }
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      return String.valueOf(value);
    }
  }
}
