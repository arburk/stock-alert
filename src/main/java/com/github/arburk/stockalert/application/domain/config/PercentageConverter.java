package com.github.arburk.stockalert.application.domain.config;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PercentageConverter {

  private PercentageConverter() {
    // Static usage only
  }

  public static Double asDouble(final String percentageAlert) {
    if (StringUtils.isBlank(percentageAlert)) {
      return null;
    }

    try {
      final double converted = Double.parseDouble(percentageAlert
          .replaceAll("[\\s%]+", "")
          .replaceAll(",+", ".")
      );
      return converted < 1
          ? converted
          : converted / 100;
    } catch (NumberFormatException e) {
      log.warn("Failed to convert percentage alert value: {}", e.getMessage());
      return null;
    }
  }
}
