package com.github.arburk.stockalert.application.service.notification;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.config.NotificationChannel;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class NotificationService {

  private final Map<Channel, NotificationSender> registry = new EnumMap<>(Channel.class);

  public NotificationService(List<NotificationSender> senders /* Spring CDI magic */) {
    if (senders == null || senders.isEmpty()) {
      log.warn("No notification senders could be registered.");
      return;
    }

    senders.forEach(sender -> {
      registry.put(sender.getChannel(), sender);
      log.debug("Registered notification sender {}", sender);
    });

  }

  public void send(final StockAlertsConfig stockAlertsConfig, final AlertConfig alertConfig, final Security latest, final Security persisted) {
    try {
      getSender(Channel.ofValue(alertConfig.notification()))
          .send(stockAlertsConfig, alertConfig, latest, persisted);
    } catch (Exception e) {
      log.error("Failed to send Alert: {}", e.getMessage());
    }
  }

  private NotificationSender getSender(Channel channel) {
    if (!registry.containsKey(channel)) {
      throw new IllegalArgumentException("No sender found for channel: " + channel);
    }
    return registry.get(channel);
  }

  public void sendPercentage(
      final StockAlertsConfig stockAlertsConfig,
      final @NonNull Security latest, final @NonNull Security persisted,
      final Double threshold, final double deviation) {

    if (stockAlertsConfig.notificationChannels() == null || stockAlertsConfig.notificationChannels().isEmpty()) {
      log.warn("No notification channels could be found.");
      return;
    }

    final List<NotificationChannel> defaultChannels = stockAlertsConfig.notificationChannels()
        .stream()
        .filter(NotificationChannel::isDefault)
        .toList();
    if (defaultChannels.isEmpty()) {
      log.warn("No default notification channel defined.");
      return;
    }

    try {
      defaultChannels.forEach(channel ->
          getSender(Channel.ofValue(channel.type()))
              .send(stockAlertsConfig, latest, persisted, threshold, deviation));
    } catch (Exception e) {
      log.error("Failed to send Percentage-Alert: {}", e.getMessage());
    }
  }
}
