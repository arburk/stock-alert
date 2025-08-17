package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StockAlertsConfig {
  private String version;

  @JsonProperty("silence-duration")
  private String silenceDuration;

  @JsonProperty("notification-channels")
  private List<NotificationChannel> notificationChannels;

  private List<SecurityConfig> securities;
}
