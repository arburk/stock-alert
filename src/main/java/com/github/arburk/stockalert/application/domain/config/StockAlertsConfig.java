package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.arburk.stockalert.application.domain.Security;
import io.micrometer.common.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockAlertsConfig(
    String version,
    @JsonProperty("silence-duration")
    String silenceDuration,
    @JsonProperty("percentage-alert")
    String percentageAlert,
    @JsonProperty("notification-channels")
    List<NotificationChannel> notificationChannels,
    List<SecurityConfig> securities
) {

  /**
   * Regex is safe: uses possessive quantifiers to prevent ReDoS.
   */
  private static final Pattern DURATION_PATTERN = Pattern.compile("^\\s*(\\d++)\\s*([dhm])\\s*$");

  public Double getPercentageAlert() {
    return PercentageConverter.asDouble(percentageAlert);
  }

  public Duration getSilenceDuration() {
    if (StringUtils.isBlank(silenceDuration)) {
      return Duration.ZERO;
    }

    final Matcher matcher = DURATION_PATTERN.matcher(silenceDuration.toLowerCase().trim());
    if (matcher.find()) {
      long value = Long.parseLong(matcher.group(1));
      final String unit = matcher.group(2);

      return switch (unit) {
        case "d" -> Duration.ofDays(value);
        case "h" -> Duration.ofHours(value);
        case "m" -> Duration.ofMinutes(value);
        default ->
            throw new IllegalArgumentException("invalid unit : " + unit + " in expression '" + silenceDuration + "'. use m(inutes), h(ours), or d(ays), e.g., 120m, 2h or 1d");
      };
    }
    throw new IllegalArgumentException("invalid format: " + silenceDuration + ". use m(inutes), h(ours), or d(ays), e.g., 120m, 2h or 1d");
  }

  public SecurityConfig findConfig(Security security) {
    return security == null || securities == null || securities.isEmpty()
        ? null
        : securities.stream().filter(security::isSameSymbolAndExchange).findFirst().orElse(null);
  }

}
