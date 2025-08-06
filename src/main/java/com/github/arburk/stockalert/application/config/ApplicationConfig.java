package com.github.arburk.stockalert.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.config.AlertConfigRoot;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

@Slf4j
@Getter
@Configuration()
@ConfigurationProperties(prefix = "stock-alert")
public class ApplicationConfig {

  private String fcsApiKey;

  @Setter
  private String baseUrl;

  @Setter
  private String updateCron;

  @Setter
  private String configUrl;

  public void setFcsApiKey(final String fcsApiKey) {
    this.fcsApiKey = (fcsApiKey) != null ? fcsApiKey.trim() : null;
  }

  @Override
  public String toString() {
    return "ApplicationConfig{" +
        "fcsApiKey=" + getMasked(fcsApiKey) +
        ",updateCron=" + updateCron +
        ",baseUrl=" + baseUrl +
        ",configUrl=" + configUrl +
        '}';
  }

  private String getMasked(final String fcsApiKey) {
    if (fcsApiKey == null) {
      return null;
    }
    final String trimmed = fcsApiKey.trim();
    final int length = trimmed.length();

    return switch (length) {
      case 0 -> "";
      case 1,2,3,4 -> "*".repeat(length);
      default -> {
        final String first = trimmed.substring(0, 2);
        final String last = trimmed.substring(length - 2);
        yield first + "*".repeat(length - 4) + last;
      }
    };
  }

  public StockAlertsConfig getStockAlertsConfig() {
    try {
      final URL input = URI.create(configUrl).normalize().toURL();
      final AlertConfigRoot alertConfigRoot = new ObjectMapper().readValue(input, AlertConfigRoot.class);
      log.debug("updated config by source '{}':\n{}", configUrl, alertConfigRoot.toString());
      return alertConfigRoot.getConfig();
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to load config %s: %s".formatted(configUrl, e.getCause()), e);
    }
  }
}
