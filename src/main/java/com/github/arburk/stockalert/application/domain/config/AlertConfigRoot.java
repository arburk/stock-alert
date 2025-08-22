package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlertConfigRoot(
  @JsonProperty("stock-alert-config")
  StockAlertsConfig config
) {
}