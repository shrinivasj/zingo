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
