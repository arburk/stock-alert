package com.github.arburk.stockalert.application.domain.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Alert {

  private double threshold;
  private String notification;

}
