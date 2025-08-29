package com.github.arburk.stockalert.application.service.notification;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;

import java.util.Arrays;

public interface NotificationSender {

  Channel getChannel();

  /**
   * Send alert based on certain security crossed currency value based threshold
   */
  void send(final StockAlertsConfig stockAlertsConfig, final Alert alert, final Security latest, final Security persisted);

  /**
   * Send alert based on percentage deviation exceeded threshold defined globally or security specific
   */
  void send(final StockAlertsConfig stockAlertsConfig, final Security latest, final Security persisted, final Double threshold, final double deviation);

  default String[] getRecipients(final StockAlertsConfig stockAlertsConfig) throws IllegalStateException {
    final var notificationChannels = stockAlertsConfig.notificationChannels();
    final var channelConfig = notificationChannels.stream()
        .filter(channel -> getChannel().getValue().equalsIgnoreCase(channel.type()))
        .findFirst()
        .orElseThrow(() ->
            new IllegalStateException("%s channel was invoked but not found in configuration"
                .formatted(getChannel().getValue())));

    return Arrays.stream(channelConfig.recipients()
            .split("[,;]"))
        .map(String::trim)
        .toArray(String[]::new);
  }

}
