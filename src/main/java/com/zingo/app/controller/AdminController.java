package com.zingo.app.controller;

import com.zingo.app.dto.AdminCafeDtos.AdminPlanListResponse;
import com.zingo.app.dto.AdminDashboardDtos.AdminDashboardResponse;
import com.zingo.app.dto.AdminCafeDtos.CreateCafePlanRequest;
import com.zingo.app.dto.AdminCafeDtos.CreateCafePlanResponse;
import com.zingo.app.dto.AdminCafeDtos.CreateTrekPlanRequest;
import com.zingo.app.exception.BadRequestException;
import com.zingo.app.service.AdminAccessService;
import com.zingo.app.service.AdminAuditService;
import com.zingo.app.service.AdminDashboardService;
import com.zingo.app.service.AdminCafeService;
import com.zingo.app.service.AdminConfigService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
  private final AdminAccessService adminAccessService;
  private final AdminCafeService adminCafeService;
  private final AdminConfigService adminConfigService;
  private final AdminDashboardService adminDashboardService;
  private final AdminAuditService adminAuditService;
  private final ConfigurableEnvironment environment;
  private final ObjectMapper objectMapper;
  private static final List<String> CONFIG_ROOTS = List.of("server", "spring", "app", "scrape", "logging");
  private static final List<String> SENSITIVE_TOKENS =
      List.of("password", "secret", "token", "apikey", "api-key", "authorization", "credential", "privatekey",
          "serverkey");

  public AdminController(AdminAccessService adminAccessService, AdminCafeService adminCafeService,
      AdminConfigService adminConfigService, AdminDashboardService adminDashboardService,
      AdminAuditService adminAuditService, ConfigurableEnvironment environment, ObjectMapper objectMapper) {
    this.adminAccessService = adminAccessService;
    this.adminCafeService = adminCafeService;
    this.adminConfigService = adminConfigService;
    this.adminDashboardService = adminDashboardService;
    this.adminAuditService = adminAuditService;
    this.environment = environment;
    this.objectMapper = objectMapper;
  }

  @GetMapping("/me")
  public AdminMeResponse me() {
    return new AdminMeResponse(adminAccessService.isCurrentUserOwner());
  }

  @GetMapping("/dashboard")
  public AdminDashboardResponse dashboard() {
    adminAccessService.assertCurrentUserIsOwner();
    return adminDashboardService.getDashboard();
  }

  @GetMapping("/config")
  public AdminConfigResponse config() {
    adminAccessService.assertCurrentUserIsOwner();

    Set<String> propertyNames = new HashSet<>();
    for (PropertySource<?> propertySource : environment.getPropertySources()) {
      if (!(propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource)) {
        continue;
      }
      for (String propertyName : enumerablePropertySource.getPropertyNames()) {
        if (isConfigProperty(propertyName)) {
          propertyNames.add(propertyName);
        }
      }
    }

    List<AdminConfigEntry> entries = new ArrayList<>();
    for (String propertyName : propertyNames) {
      Object rawValue = resolveRawPropertyValue(propertyName);
      String value = stringify(rawValue);
      entries.add(new AdminConfigEntry(propertyName, maskIfSensitive(propertyName, value)));
    }
    entries.sort(Comparator.comparing(AdminConfigEntry::key));
    return new AdminConfigResponse(entries);
  }

  @PostMapping("/config")
  public AdminConfigEntry updateConfig(@RequestBody AdminConfigUpdateRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    if (request == null || request.key() == null || request.key().isBlank()) {
      throw new BadRequestException("key is required");
    }
    String key = request.key().trim();
    if (!isConfigProperty(key)) {
      throw new BadRequestException("Unsupported config key: " + key);
    }
    adminConfigService.putOverride(key, request.value() == null ? "" : request.value());

    Object rawValue = resolveRawPropertyValue(key);
    String value = stringify(rawValue);
    adminAuditService.logActivity("CONFIG_UPDATE", "Config updated: " + key, "Saved application config for " + key);
    return new AdminConfigEntry(key, maskIfSensitive(key, value));
  }

  @GetMapping("/cafes")
  public AdminPlanListResponse cafePlans() {
    adminAccessService.assertCurrentUserIsOwner();
    return new AdminPlanListResponse(adminCafeService.listCafePlans());
  }

  @PostMapping("/cafes")
  public CreateCafePlanResponse createCafePlan(@RequestBody CreateCafePlanRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    return adminCafeService.createCafePlan(request);
  }

  @PutMapping("/cafes/{showtimeId}")
  public CreateCafePlanResponse updateCafePlan(@PathVariable Long showtimeId, @RequestBody CreateCafePlanRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    return adminCafeService.updateCafePlan(showtimeId, request);
  }

  @DeleteMapping("/cafes/{showtimeId}")
  public void deleteCafePlan(@PathVariable Long showtimeId) {
    adminAccessService.assertCurrentUserIsOwner();
    adminCafeService.deleteCafePlan(showtimeId);
  }

  @GetMapping("/treks")
  public AdminPlanListResponse trekPlans() {
    adminAccessService.assertCurrentUserIsOwner();
    return new AdminPlanListResponse(adminCafeService.listTrekPlans());
  }

  @PostMapping("/treks")
  public CreateCafePlanResponse createTrekPlan(@RequestBody CreateTrekPlanRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    return adminCafeService.createTrekPlan(request);
  }

  @PutMapping("/treks/{showtimeId}")
  public CreateCafePlanResponse updateTrekPlan(@PathVariable Long showtimeId, @RequestBody CreateTrekPlanRequest request) {
    adminAccessService.assertCurrentUserIsOwner();
    return adminCafeService.updateTrekPlan(showtimeId, request);
  }

  @DeleteMapping("/treks/{showtimeId}")
  public void deleteTrekPlan(@PathVariable Long showtimeId) {
    adminAccessService.assertCurrentUserIsOwner();
    adminCafeService.deleteTrekPlan(showtimeId);
  }

  private boolean isConfigProperty(String propertyName) {
    for (String root : CONFIG_ROOTS) {
      if (propertyName.equals(root) || propertyName.startsWith(root + ".")) {
        return true;
      }
    }
    return false;
  }

  private String maskIfSensitive(String propertyName, String value) {
    if (value == null) {
      return "";
    }
    String normalized = propertyName.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    for (String token : SENSITIVE_TOKENS) {
      String normalizedToken = token.replace("-", "");
      if (normalized.contains(normalizedToken)) {
        return "********";
      }
    }
    return value;
  }

  private Object resolveRawPropertyValue(String propertyName) {
    for (PropertySource<?> propertySource : environment.getPropertySources()) {
      Object value = propertySource.getProperty(propertyName);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String stringify(Object value) {
    if (value == null) {
      return "";
    }
    if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean || value instanceof Enum) {
      return String.valueOf(value);
    }
    if (value.getClass().isArray()) {
      int length = Array.getLength(value);
      List<Object> items = new ArrayList<>(length);
      for (int index = 0; index < length; index++) {
        items.add(Array.get(value, index));
      }
      return toJson(items);
    }
    return toJson(value);
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      return String.valueOf(value);
    }
  }

  public record AdminMeResponse(boolean owner) {}
  public record AdminConfigEntry(String key, String value) {}
  public record AdminConfigResponse(List<AdminConfigEntry> entries) {}
  public record AdminConfigUpdateRequest(String key, String value) {}
}
