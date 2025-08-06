package com.github.arburk.stockalert.application.service.notification;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
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

  public void send(final Alert alert, final Security latest, final Security persisted) {
    try {
      getSender(Channel.ofValue(alert.getNotification()))
          .send(alert, latest, persisted);
    } catch (Exception e) {
      log.error(e.getMessage());
      // TODO: do we have to handle this any further
    }
  }

  private NotificationSender getSender(Channel channel) {
    if (!registry.containsKey(channel)) {
      throw new IllegalArgumentException("No sender found for channel: " + channel);
    }
    return registry.get(channel);
  }

}
