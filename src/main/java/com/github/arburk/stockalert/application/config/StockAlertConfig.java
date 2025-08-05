package com.github.arburk.stockalert.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration()
@ConfigurationProperties(prefix = "stock-alert")
public class StockAlertConfig {

  @Value("${fcs-api-key}")
  private String fcsApiKey;

  public void setFcsApiKey(final String fcsApiKey) {
    this.fcsApiKey = (fcsApiKey) != null ? fcsApiKey.trim() : null;
  }

  @Override
  public String toString() {
    return "StockAlertConfig{" +
        "fcsApiKey=" + getMasked(fcsApiKey) +
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
}
