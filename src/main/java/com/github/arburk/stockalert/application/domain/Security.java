package com.github.arburk.stockalert.application.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record Security(
    String symbol,
    Double price,
    String currency,
    Double changePercentage,
    LocalDateTime timestamp,
    String exchange
) {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @JsonIgnore
  public String getTimestampFormatted() {
    return this.timestamp == null
        ? null
        : timestamp.format(FORMATTER);
  }
}
