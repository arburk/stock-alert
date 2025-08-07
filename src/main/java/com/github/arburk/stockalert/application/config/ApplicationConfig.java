package com.github.arburk.stockalert.application.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.config.AlertConfigRoot;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import io.micrometer.common.util.StringUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

@Slf4j
@Getter
@Configuration
@ConfigurationProperties(prefix = "stock-alert")
public class ApplicationConfig {

  private String fcsApiKey;

  @Setter
  private String baseUrl;

  @Setter
  private String updateCron;

  @Setter
  private String configUrl;

  @Getter(AccessLevel.NONE)
  private final ObjectMapper objectMapper;

  public ApplicationConfig(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

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
      final AlertConfigRoot alertConfigRoot = objectMapper.readValue(getConfigFileAsUrl(), AlertConfigRoot.class);
      log.debug("updated config by source '{}':\n{}", configUrl, alertConfigRoot.toString());
      return alertConfigRoot.getConfig();
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to load config %s: %s".formatted(configUrl, e.getCause()), e);
    }
  }

  private URL getConfigFileAsUrl() throws MalformedURLException {
    if (this.configUrl == null || StringUtils.isBlank(this.configUrl)) {
      throw new IllegalArgumentException("Configuration is missing. Please set CONFIG-URL as environment variable.");
    }

    // check for valid pathes
    if (this.configUrl.startsWith("file://")
        || this.configUrl.startsWith("http://")
        || this.configUrl.startsWith("https://")) {
      return URI.create(configUrl).normalize().toURL();
    }

    return Path.of(this.configUrl).normalize().toAbsolutePath().toUri().toURL();
  }
}
