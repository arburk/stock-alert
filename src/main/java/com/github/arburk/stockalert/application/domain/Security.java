package com.github.arburk.stockalert.application.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import java.util.Locale;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record Security(
    String symbol,
    Double price,
    String currency,
    Double changePercentage,
    LocalDateTime timestamp,
    String exchange,
    Collection<Alert> alertLog,
    String name) {

  public Security {
    if(alertLog == null) {
      // ensure never empty so new entries can be added
      alertLog = new ArrayList<>();
    }
  }

  /**
   * Convenience constructor for securities without a known (full) name.
   */
  public Security(String symbol, Double price, String currency, Double changePercentage,
                  LocalDateTime timestamp, String exchange, Collection<Alert> alertLog) {
    this(symbol, price, currency, changePercentage, timestamp, exchange, alertLog, null);
  }

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public static @NonNull Security fromConfig(final SecurityConfig configElement) {
    return new Security(configElement.symbol(), null, null, null, null, configElement.exchange(), null);
  }

  public static String formatPercentage(final double percentage) {
    return String.format(Locale.ROOT, "%.2f %%", percentage * 100);
  }

  /**
   * Returns a human-readable label for notifications, e.g. {@code ING Groep N.V. (INGA.AS)}.
   * Falls back to the symbol only if no name is known.
   */
  @JsonIgnore
  public String getDisplayName() {
    if (name == null || name.isBlank()) {
      return symbol;
    }
    return "%s (%s)".formatted(name.trim(), symbol);
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
      final List<Alert> alertToReplace = alertLog.stream()
          .filter(log -> log.equals(alert)).toList();
      alertToReplace.forEach(alertLog::remove);
    }
    alertLog.add(alert);
  }
}
