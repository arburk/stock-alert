package com.github.arburk.stockalert.infrastructure.notification;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.application.service.notification.Channel;
import com.github.arburk.stockalert.application.service.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoopNotificationSender implements NotificationSender {

  @Override
  public Channel getChannel() {
    return Channel.NOOP;
  }

  @Override
  public void send(final StockAlertsConfig stockAlertsConfig, final AlertConfig alertConfig, final Security latest, final Security persisted) {
    final String currency = latest.currency();
    log.info("Skip sending alert for {} with threshold {} {} sice stock price moved from {} {} to {} {} at {}",
        latest.symbol(), currency, alertConfig.threshold(), currency, persisted.price(), currency, latest.price(), latest.timestamp());
  }

  @Override
  public void send(final StockAlertsConfig stockAlertsConfig, final Security latest, final Security persisted, final Double threshold, final double deviation) {
    final String currency = latest.currency();
    log.info("Skip sending alert for {} with percentage threshold {}. Stock price moved from {} {} to {} {} at {} resulting in {}%",
        latest.symbol(), threshold, persisted.price(), currency, latest.price(), currency, latest.timestamp(), deviation);
  }
}
