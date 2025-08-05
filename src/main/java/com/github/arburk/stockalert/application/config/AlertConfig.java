package com.github.arburk.stockalert.application.config;

import com.github.arburk.stockalert.application.domain.Alert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Slf4j
@Component
public class AlertConfig {
  public Collection<Alert> getAlerts() {
    //TODO: implement me
    log.error("implement StockService#getAlerts");
    return null;
  }
}
