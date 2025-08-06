package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StockService {

  final Provider provider;
  final ApplicationConfig applicationConfig;

  public StockService(Provider provider, ApplicationConfig applicationConfig) {
    this.provider = provider;
    this.applicationConfig = applicationConfig;
  }

  public void update() {
    final List<SecurityConfig> alertConfig = this.applicationConfig.getStockAlertsConfig().getSecurities();
    final Set<String> symbolsToQueryFor = alertConfig.stream().map(SecurityConfig::getSymbol).collect(Collectors.toSet());
    raiseAlerts(alertConfig, provider.getLatest(symbolsToQueryFor));
  }

  private void raiseAlerts(final List<SecurityConfig> alertConfig, final Collection<Security> latestSecurities) {
    //TODO: implement me
    log.error("implement StockService#update");
  }
}
