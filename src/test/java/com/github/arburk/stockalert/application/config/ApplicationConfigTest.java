package com.github.arburk.stockalert.application.config;

import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.domain.config.NotificationChannel;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
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
    assertEquals("ApplicationConfig{fcsApiKey=null,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());
  }

  @Test
  void keyIsMasked_Empty() {
    testee.setFcsApiKey("");
    assertEquals("", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey("  ");
    assertEquals("", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());
  }

  @Test
  void keyIsFullyMasked() {
    testee.setFcsApiKey("A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey(" A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey(" A ");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=*,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey("AB");
    assertEquals("AB", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=**,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey("ABc");
    assertEquals("ABc", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=***,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey("ABcD");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=****,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());

    testee.setFcsApiKey(" ABcD ");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=****,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());
  }

  @Test
  void keyIsPartiallyMasked() {
    testee.setFcsApiKey("MyApiKey");
    assertEquals("MyApiKey", testee.getFcsApiKey());
    assertEquals("ApplicationConfig{fcsApiKey=My****ey,updateCron=* 16 9-21 * * MON-FRI,baseUrl=https://stock-alert.io,configUrl=null}", testee.toString());
  }

  @Nested
  class StockAlertsConfigTest {

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
      assertEquals("0.0.1", stockAlertsConfig.getVersion());
      assertEquals("6h", stockAlertsConfig.getSilenceDuration());
      final List<NotificationChannel> notificationChannels = stockAlertsConfig.getNotificationChannels();
      assertNotNull(notificationChannels);
      assertEquals(1, notificationChannels.size());
      assertEquals("email", notificationChannels.getFirst().getType());
      assertEquals("me@company.com", notificationChannels.getFirst().getRecipients());
      assertTrue(notificationChannels.getFirst().isUseOnError());
      final List<SecurityConfig> securities = stockAlertsConfig.getSecurities();
      assertEquals(1, securities.size());
      final SecurityConfig first = securities.getFirst();
      assertEquals("BALN", first.getSymbol());
      assertEquals("Switzerland", first.getExchange());
      assertAlerts(first.getAlerts());
    }

    private void assertAlerts(final List<Alert> alerts) {
      assertEquals(3, alerts.size());
      final Alert first = alerts.getFirst();
      final Alert second = alerts.get(1);
      final Alert third = alerts.getLast();

      assertAll(
          () -> assertEquals(200.00, first.getThreshold()),
          () -> assertEquals("email", first.getNotification()),
          () -> assertEquals(220.00, second.getThreshold()),
          () -> assertEquals("sms", second.getNotification()),
          () -> assertEquals(185.00, third.getThreshold()),
          () -> assertEquals("email", third.getNotification())
      );
    }
  }
}