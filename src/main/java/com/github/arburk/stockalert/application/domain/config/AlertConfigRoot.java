package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertConfigRoot {

  @JsonProperty("stock-alert-config")
  private StockAlertsConfig config;

}