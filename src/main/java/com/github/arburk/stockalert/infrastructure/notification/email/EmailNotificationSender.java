package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.service.notification.Channel;
import com.github.arburk.stockalert.application.service.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

  @Override
  public Channel getChannel() {
    return Channel.EMAIL;
  }

  @Override
  public void send(final Alert alert, final Security latest, final Security persisted) {
    //TODO: implement me
    throw new NotImplementedException("Implement EmailNotificationSender");
  }
}
