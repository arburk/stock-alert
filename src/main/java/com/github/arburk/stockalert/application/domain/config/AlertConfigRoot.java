package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AlertConfigRoot {

  @JsonProperty("stock-alert-config")
  private StockAlertsConfig config;

}