package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.arburk.stockalert.application.domain.Alert;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertConfig(
    double threshold,
    String notification,
    String _comment
) {

  public Alert asAlert(String unit) {
    return new Alert(LocalDateTime.now(), threshold, unit);
  }
}
