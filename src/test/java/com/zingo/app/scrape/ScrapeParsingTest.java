package com.zingo.app.scrape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class ScrapeParsingTest {

  @Test
  void slugifyMapsKnownCityAliases() {
    assertEquals("bengaluru", ScrapeParsing.slugify("Bangalore"));
    assertEquals("mumbai", ScrapeParsing.slugify("Bombay"));
    assertEquals("kolkata", ScrapeParsing.slugify("Calcutta"));
  }

  @Test
  void slugifyHandlesNormalInputAndBlankValues() {
    assertEquals("new-delhi", ScrapeParsing.slugify("New Delhi"));
    assertNull(ScrapeParsing.slugify("   "));
    assertNull(ScrapeParsing.slugify(null));
  }
}
