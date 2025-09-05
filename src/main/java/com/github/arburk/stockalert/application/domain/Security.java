package com.github.arburk.stockalert.application.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

  public Security {
    if(alertLog == null) {
      // ensure never empty so new entries can be added
      alertLog = new ArrayList<>();
    }
  }

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static @NonNull Security fromConfig(final SecurityConfig configElement) {
    return new Security(configElement.symbol(), null, null, null, null, configElement.exchange(), null);
  }

  public static String formatPercentage(final double percentage) {
    return String.format("%.2f %%", percentage * 100);
  }

  @JsonIgnore
  public String getTimestampFormatted() {
    return this.timestamp == null
        ? null
        : timestamp.format(FORMATTER);
  }

  public boolean isSameSymbolAndExchange(SecurityConfig securityConfig) {
    return securityConfig != null
        && Objects.equals(symbol, securityConfig.symbol())
        && Objects.equals(exchange, securityConfig.exchange());
  }

  @Override
  public boolean equals(final Object o) {
    return switch (o) {
      case Security security -> Objects.equals(symbol, security.symbol) && Objects.equals(exchange, security.exchange);
      case null, default -> false;
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(symbol, price, currency, changePercentage, timestamp, exchange, alertLog);
  }

  public void addLog(final Alert alert) {
    if (!alertLog.isEmpty()) {
      // remove entry before adding to update timestamp only
      alertLog.stream()
          .filter(log -> log.equals(alert))
          .sorted()
          .findFirst().ifPresent(alertLog::remove);
    }
    alertLog.add(alert);
  }
}
