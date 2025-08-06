package com.github.arburk.stockalert.application.domain.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Alert {

  private double threshold;
  private String notification;

}
