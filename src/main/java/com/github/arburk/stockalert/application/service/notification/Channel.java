package com.github.arburk.stockalert.application.service.notification;

import java.util.Arrays;

public enum Channel {
  NOOP("noop"),
  EMAIL("email");

  private String value;

  Channel(String value) {
    this.value = value;
  }

  public static Channel ofValue(final String notification) {
    return Arrays.stream(Channel.values())
        .filter(channel -> channel.value.equals(notification))
        .findFirst().orElse(NOOP);
  }
}
