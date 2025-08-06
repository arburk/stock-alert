package com.github.arburk.stockalert.application.service.notification;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;

import java.util.Arrays;

public interface NotificationSender {

  Channel getChannel();

  void send(final Alert alert, final Security latest, final Security persisted);

  default String[] getRecipients(ApplicationConfig appConfig) throws IllegalStateException {
    final var notificationChannels = appConfig.getStockAlertsConfig().getNotificationChannels();
    final var channelConfig = notificationChannels.stream()
        .filter(channel -> getChannel().getValue().equalsIgnoreCase(channel.getType()))
        .findFirst()
        .orElseThrow(() ->
            new IllegalStateException("%s channel was invoked but not found in configuration"
                .formatted(getChannel().getValue())));

    return Arrays.stream(channelConfig.getRecipients()
            .split("[,;]"))
        .map(String::trim)
        .toArray(String[]::new);
  }

}
