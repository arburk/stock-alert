package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SecurityConfig(
    String symbol,
    String exchange,
    String isin,
    String _comment,
    @JsonProperty("percentage-alert")
    String percentageAlert,
    List<AlertConfig> alerts
) {
  public Double getPercentageAlert() {
    return PercentageConverter.asDouble(percentageAlert);
  }
}
