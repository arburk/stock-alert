package com.github.arburk.stockalert.application.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;

public record Security(
    String symbol,
    Double price,
    String currency,
    Double changePercentage,
    LocalDateTime timestamp,
    String exchange,
    Collection<Alert> alertLog) {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static @NonNull Security fromConfig(final SecurityConfig configElement) {
    return new Security(configElement.symbol(), null, null, null, null, configElement.exchange(), null);
  }

  @JsonIgnore
  public String getTimestampFormatted() {
    return this.timestamp == null
        ? null
        : timestamp.format(FORMATTER);
  }

  @Override
  public boolean equals(final Object o) {
    return switch (o) {
      case Security security -> Objects.equals(symbol, security.symbol) && Objects.equals(exchange, security.exchange);
      case SecurityConfig securityConfig ->
          Objects.equals(symbol, securityConfig.symbol()) && Objects.equals(exchange, securityConfig.exchange());
      case null, default -> false;
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, price, currency, changePercentage, timestamp, exchange, alertLog);
  }

}
