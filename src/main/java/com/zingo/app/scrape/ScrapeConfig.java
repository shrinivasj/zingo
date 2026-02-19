package com.zingo.app.scrape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scrape")
public class ScrapeConfig {
  private boolean enabled = true;
  private String cron = "0 0 12 * * *";
  private String zone = "Asia/Kolkata";
  private int days = 7;
  private int maxDetailPages = 25;
  private String userAgent =
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36";
  private List<String> pincodes = new ArrayList<>();
  private Map<String, String> postalCodeCityMap = new HashMap<>();
  private Providers providers = new Providers();
  private PincodeLookup pincodeLookup = new PincodeLookup();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public int getDays() {
    return days;
  }

  public void setDays(int days) {
    this.days = days;
  }

  public int getMaxDetailPages() {
    return maxDetailPages;
  }

  public void setMaxDetailPages(int maxDetailPages) {
    this.maxDetailPages = maxDetailPages;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  public List<String> getPincodes() {
    return pincodes;
  }

  public void setPincodes(List<String> pincodes) {
    this.pincodes = pincodes;
  }

  public Map<String, String> getPostalCodeCityMap() {
    return postalCodeCityMap;
  }

  public void setPostalCodeCityMap(Map<String, String> postalCodeCityMap) {
    this.postalCodeCityMap = postalCodeCityMap;
  }

  public Providers getProviders() {
    return providers;
  }

  public void setProviders(Providers providers) {
    this.providers = providers;
  }

  public PincodeLookup getPincodeLookup() {
    return pincodeLookup;
  }

  public void setPincodeLookup(PincodeLookup pincodeLookup) {
    this.pincodeLookup = pincodeLookup;
  }

  public static class Providers {
    private BookMyShow bookmyshow = new BookMyShow();
    private District district = new District();
    private MovieGlu movieglu = new MovieGlu();

    public BookMyShow getBookmyshow() {
      return bookmyshow;
    }

    public void setBookmyshow(BookMyShow bookmyshow) {
      this.bookmyshow = bookmyshow;
    }

    public District getDistrict() {
      return district;
    }

    public void setDistrict(District district) {
      this.district = district;
    }

    public MovieGlu getMovieglu() {
      return movieglu;
    }

    public void setMovieglu(MovieGlu movieglu) {
      this.movieglu = movieglu;
    }
  }

  public static class BookMyShow {
    private boolean enabled = true;
    private String baseUrl = "https://in.bookmyshow.com";
    private String moviesPathTemplate = "/explore/movies-{citySlug}";
    private String eventsPathTemplate = "/explore/events-{citySlug}";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getMoviesPathTemplate() {
      return moviesPathTemplate;
    }

    public void setMoviesPathTemplate(String moviesPathTemplate) {
      this.moviesPathTemplate = moviesPathTemplate;
    }

    public String getEventsPathTemplate() {
      return eventsPathTemplate;
    }

    public void setEventsPathTemplate(String eventsPathTemplate) {
      this.eventsPathTemplate = eventsPathTemplate;
    }
  }

  public static class District {
    private boolean enabled = true;
    private String baseUrl = "https://www.district.in";
    private String moviesPathTemplate = "/movies/{citySlug}-movie-tickets";
    private String eventsPathTemplate = "/activities/{citySlug}-activity-tickets";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public String getMoviesPathTemplate() {
      return moviesPathTemplate;
    }

    public void setMoviesPathTemplate(String moviesPathTemplate) {
      this.moviesPathTemplate = moviesPathTemplate;
    }

    public String getEventsPathTemplate() {
      return eventsPathTemplate;
    }

    public void setEventsPathTemplate(String eventsPathTemplate) {
      this.eventsPathTemplate = eventsPathTemplate;
    }
  }

  public static class MovieGlu {
    private boolean enabled = false;
    private String baseUrl = "https://api-gate2.movieglu.com/";
    private boolean useSandbox = false;
    private String client = "";
    private String apiKey = "";
    private String authorization = "";
    private String territory = "IN";
    private String apiVersion = "v201";
    private String geolocation = "20.59;78.96";
    private String sandboxClient = "";
    private String sandboxApiKey = "";
    private String sandboxAuthorization = "";
    private String sandboxTerritory = "XX";
    private String sandboxApiVersion = "v201";
    private String sandboxGeolocation = "-22.0;14.0";
    private int maxCinemas = 10;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }

    public boolean isUseSandbox() {
      return useSandbox;
    }

    public void setUseSandbox(boolean useSandbox) {
      this.useSandbox = useSandbox;
    }

    public String getClient() {
      return client;
    }

    public void setClient(String client) {
      this.client = client;
    }

    public String getApiKey() {
      return apiKey;
    }

    public void setApiKey(String apiKey) {
      this.apiKey = apiKey;
    }

    public String getAuthorization() {
      return authorization;
    }

    public void setAuthorization(String authorization) {
      this.authorization = authorization;
    }

    public String getTerritory() {
      return territory;
    }

    public void setTerritory(String territory) {
      this.territory = territory;
    }

    public String getApiVersion() {
      return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
      this.apiVersion = apiVersion;
    }

    public String getGeolocation() {
      return geolocation;
    }

    public void setGeolocation(String geolocation) {
      this.geolocation = geolocation;
    }

    public String getSandboxClient() {
      return sandboxClient;
    }

    public void setSandboxClient(String sandboxClient) {
      this.sandboxClient = sandboxClient;
    }

    public String getSandboxApiKey() {
      return sandboxApiKey;
    }

    public void setSandboxApiKey(String sandboxApiKey) {
      this.sandboxApiKey = sandboxApiKey;
    }

    public String getSandboxAuthorization() {
      return sandboxAuthorization;
    }

    public void setSandboxAuthorization(String sandboxAuthorization) {
      this.sandboxAuthorization = sandboxAuthorization;
    }

    public String getSandboxTerritory() {
      return sandboxTerritory;
    }

    public void setSandboxTerritory(String sandboxTerritory) {
      this.sandboxTerritory = sandboxTerritory;
    }

    public String getSandboxApiVersion() {
      return sandboxApiVersion;
    }

    public void setSandboxApiVersion(String sandboxApiVersion) {
      this.sandboxApiVersion = sandboxApiVersion;
    }

    public String getSandboxGeolocation() {
      return sandboxGeolocation;
    }

    public void setSandboxGeolocation(String sandboxGeolocation) {
      this.sandboxGeolocation = sandboxGeolocation;
    }

    public int getMaxCinemas() {
      return maxCinemas;
    }

    public void setMaxCinemas(int maxCinemas) {
      this.maxCinemas = maxCinemas;
    }
  }

  public static class PincodeLookup {
    private boolean enabled = true;
    private String baseUrl = "https://api.postalpincode.in/pincode/";

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}
