package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockAlertsConfig(
    String version,
  @JsonProperty("silence-duration")
  String silenceDuration,
  @JsonProperty("notification-channels")
    List<NotificationChannel> notificationChannels,
    List<SecurityConfig> securities
) {
}
