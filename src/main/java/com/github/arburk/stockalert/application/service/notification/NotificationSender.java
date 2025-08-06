package com.github.arburk.stockalert.application.service.notification;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;

public interface NotificationSender {

  Channel getChannel();
  void send(final Alert alert, final Security latest, final Security persisted);

}
