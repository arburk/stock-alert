package com.github.arburk.stockalert.application.config;

import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.config.NotificationChannel;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationConfigTest {

  private ApplicationConfig testee;

  @BeforeEach
  void setUp() {
    testee = new ApplicationConfig(new JacksonConfig().objectMapper());
    testee.setBaseUrl("https://stock-alert.io");
    testee.setUpdateCron("* 16 9-21 * * MON-FRI");
  }

  @Test
  void keyIsMasked_Null() {
    testee.setFcsApiKey(null);
    assertNull(testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=null,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());
  }

  @Test
  void keyIsMasked_Empty() {
    final String expedted = "ApplicationConfig{fcsApiKey=,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}";

    testee.setFcsApiKey("");
    assertEquals("", testee.getFcsApiKey());
    assertEquals(expedted, testee.toString());

    testee.setFcsApiKey("  ");
    assertEquals("", testee.getFcsApiKey());
    assertEquals(expedted, testee.toString());
  }

  @Test
  void keyIsFullyMasked() {
    testee.setFcsApiKey("A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey(" A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey(" A ");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey("AB");
    assertEquals("AB", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=**,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey("ABc");
    assertEquals("ABc", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=***,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey("ABcD");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=****,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());

    testee.setFcsApiKey(" ABcD ");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=****,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());
  }

  @Test
  void keyIsPartiallyMasked() {
    testee.setFcsApiKey("MyApiKey");
    assertEquals("MyApiKey", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=My****ey,updateCron=* 16 9-21 * * MON-FRI,baseUrl='https://stock-alert.io',configUrl='null',runOnStartup=false}", testee.toString());
  }

  @Nested
  class StockAlertsConfigTest {

    @Test
    void getSilenceDuration_HappyFlows() {
      assertAll(
          () -> assertEquals(Duration.ZERO, assertDoesNotThrow((() -> new StockAlertsConfig(null, null, null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.ZERO, assertDoesNotThrow(() -> new StockAlertsConfig(null, "", null, null, null).getSilenceDuration())),
          () -> assertEquals(Duration.ZERO, assertDoesNotThrow(() -> new StockAlertsConfig(null, "  ", null, null, null).getSilenceDuration())),

          () -> assertEquals(Duration.of(1, ChronoUnit.MINUTES), assertDoesNotThrow((() -> new StockAlertsConfig(null, "1m", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(1, ChronoUnit.MINUTES), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 1m ", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(1, ChronoUnit.MINUTES), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 1  m  ", null, null, null).getSilenceDuration()))),

          () -> assertEquals(Duration.of(6, ChronoUnit.HOURS), assertDoesNotThrow((() -> new StockAlertsConfig(null, "6h", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(6, ChronoUnit.HOURS), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 6h ", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(6, ChronoUnit.HOURS), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 6  h  ", null, null, null).getSilenceDuration()))),

          () -> assertEquals(Duration.of(2, ChronoUnit.DAYS), assertDoesNotThrow((() -> new StockAlertsConfig(null, "2d", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(2, ChronoUnit.DAYS), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 2d ", null, null, null).getSilenceDuration()))),
          () -> assertEquals(Duration.of(2, ChronoUnit.DAYS), assertDoesNotThrow((() -> new StockAlertsConfig(null, " 2  d  ", null, null, null).getSilenceDuration())))
      );
    }

    @ParameterizedTest
    @ValueSource(strings = {"2days", "2 Days", "2"})
    void getSilenceDuration_UnhappyFlows(String input) {
      final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, input, null, null, null);
      final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, stockAlertsConfig::getSilenceDuration);
      assertEquals("invalid format: " + input + ". use m(inutes), h(ours), or d(ays), e.g., 120m, 2h or 1d",
          exception.getMessage());
    }

    @Test
    void file_HappyFlow() {
      testee.setConfigUrl(Path.of("src/main/resources/config-example.json").toUri().toString());
      assertConfigExample(testee.getStockAlertsConfig());
    }

    @Test
    void url_HappyFlow() {
      testee.setConfigUrl("https://raw.githubusercontent.com/arburk/stock-alert/refs/heads/main/src/main/resources/config-example.json");
      assertConfigExample(testee.getStockAlertsConfig());
    }

    @Test
    void windowsPath_HappyFlow() {
      testee.setConfigUrl("C:\\stock-alert\\config\\config.json");

      final var exception = assertThrows(IllegalArgumentException.class, () -> testee.getStockAlertsConfig());

      final Throwable cause = exception.getCause();
      assertFalse(cause instanceof IllegalArgumentException, "IllegalArgumentException not expected here");
      assertFalse(cause.getMessage().startsWith("Illegal character in opaque part"));
    }

    @Test
    void windowsPathWithSlashes_HappyFlow() {
      testee.setConfigUrl("C:/stock-alert/config/config.json");

      final var exception = assertThrows(IllegalArgumentException.class, () -> testee.getStockAlertsConfig());

      final Throwable cause = exception.getCause();
      assertFalse(cause instanceof MalformedURLException, "MalformedURLException not expected here");
      assertFalse(cause.getMessage().startsWith("unknown protocol: c"));
    }

    private void assertConfigExample(final StockAlertsConfig stockAlertsConfig) {
      assertNotNull(stockAlertsConfig);
      assertEquals("0.2.1-SNAPSHOT", stockAlertsConfig.version());
      assertEquals("6h", stockAlertsConfig.silenceDuration());
      assertEquals("5%", stockAlertsConfig.percentageAlert());
      assertEquals(.05, stockAlertsConfig.getPercentageAlert());
      final List<NotificationChannel> notificationChannels = stockAlertsConfig.notificationChannels();
      assertNotNull(notificationChannels);
      assertEquals(1, notificationChannels.size());
      assertEquals("email", notificationChannels.getFirst().type());
      assertEquals("me@company.com", notificationChannels.getFirst().recipients());
      assertTrue(notificationChannels.getFirst().useOnError());
      final List<SecurityConfig> securities = stockAlertsConfig.securities();
      assertEquals(1, securities.size());
      final SecurityConfig first = securities.getFirst();
      assertEquals("BALN", first.symbol());
      assertEquals("Switzerland", first.exchange());
      assertEquals("CH0012410517", first.isin());
      assertEquals("'symbol' and 'exchange' must match the FCSAPI symbol definition; 'isin' -> not used by FCSAPI; 'percentage-alert' -> overrides the stock-alert-config.percentage-alert. empty value resets this alert", first._comment());
      assertEquals("10", first.percentageAlert());
      assertEquals(.1, first.getPercentageAlert());
      assertAlerts(first.alerts());
    }

    private void assertAlerts(final List<AlertConfig> alertConfigs) {
      assertEquals(3, alertConfigs.size());
      final AlertConfig first = alertConfigs.getFirst();
      final AlertConfig second = alertConfigs.get(1);
      final AlertConfig third = alertConfigs.getLast();

      assertAll(
          () -> assertEquals(200.00, first.threshold()),
          () -> assertEquals("email", first.notification()),
          () -> assertEquals(220.00, second.threshold()),
          () -> assertEquals("sms", second.notification()),
          () -> assertEquals(185.00, third.threshold()),
          () -> assertEquals("email", third.notification())
      );
    }
  }
}