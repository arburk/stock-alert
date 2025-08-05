package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.Alert;
import com.github.arburk.stockalert.application.domain.Security;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StockService {

  final Provider provider;
  final AlertConfig alertConfig;

  public StockService(AlertConfig alertConfig, Provider provider) {
    this.provider = provider;
    this.alertConfig = alertConfig;
  }

  public void update() {
    final Collection<Alert> alertConfig = this.alertConfig.getAlerts();
    final Set<String> symbolsToQueryFor = alertConfig.stream().map(Alert::symbol).collect(Collectors.toSet());
    raiseAlerts(alertConfig, provider.getLatest(symbolsToQueryFor));
  }

  private void raiseAlerts(final Collection<Alert> alertConfig, final Collection<Security> latestSecurities) {
    //TODO: implement me
    log.error("implement StockService#update");
  }
}
