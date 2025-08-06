package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StockService {

  final ApplicationConfig applicationConfig;
  final StockProvider stockProvider;
  final PersistanceProvider persistanceProvider;
  final NotificationService notificationService;

  public StockService(ApplicationConfig applicationConfig, StockProvider stockProvider, PersistanceProvider persistanceProvider, NotificationService notificationService) {
    this.stockProvider = stockProvider;
    this.applicationConfig = applicationConfig;
    this.persistanceProvider = persistanceProvider;
    this.notificationService = notificationService;
  }

  public void update() {
    log.info("Updating stock alert config...");
    final List<SecurityConfig> alertConfig = this.applicationConfig.getStockAlertsConfig().getSecurities();
    final Set<String> symbolsToQueryFor = alertConfig.stream().map(SecurityConfig::getSymbol).collect(Collectors.toSet());
    log.debug("perform and process update for {}", symbolsToQueryFor);
    process(alertConfig, stockProvider.getLatest(symbolsToQueryFor));
  }

  private void process(final List<SecurityConfig> alertConfig, final Collection<Security> latestSecurities) {
    checkForCompleteness(alertConfig, latestSecurities);
    final Collection<Security> persistedSecurites = persistanceProvider.getSecurites();
    alertConfig.forEach(configElement -> checkAndRaiseAlert(
        configElement,
        getSecurity(latestSecurities, configElement),
        getSecurity(persistedSecurites, configElement)
    ));
    updatePersistedSecurities(persistedSecurites, latestSecurities);
  }

  private void checkForCompleteness(final List<SecurityConfig> alertConfig, final Collection<Security> latestSecurities) {
    final Set<String> alertConfigured = alertConfig.stream().map(
            security -> "%s:%s".formatted(security.getSymbol(), security.getExchange()))
        .sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    final Set<String> retrieved = latestSecurities.stream().map(
            security -> "%s:%s".formatted(security.symbol(), security.exchange()))
        .sorted().collect(Collectors.toCollection(LinkedHashSet::new));
    if (alertConfigured.equals(retrieved)) {
      log.debug("Found all stocks configured in alert config.");
      return;
    }
    log.warn("Did not find all stocks configured in alert config.\nconfigured: {}\nretrieved: {}", alertConfigured, retrieved);

    //TODO: send warning to check config?
  }

  private Security getSecurity(final Collection<Security> securities, final SecurityConfig securityConfig) {
    if (securities == null || securities.isEmpty()) {
      return null;
    }

    return securities.stream().filter(security ->
            security.symbol().equals(securityConfig.getSymbol())
                && security.exchange().equals(securityConfig.getExchange()))
        .findFirst().orElse(null);
  }

  private void updatePersistedSecurities(final Collection<Security> persistedSecurities, final Collection<Security> latestSecurities) {
    if (latestSecurities == null || latestSecurities.isEmpty()) {
      log.debug("skip updating persisted securities.");
      return;
    }

    latestSecurities.forEach(latest -> {
      persistedSecurities.stream().filter(security ->
              security.symbol().equals(latest.symbol()) && security.exchange().equals(latest.exchange()))
          .findFirst().ifPresent(persistedSecurities::remove);
      persistedSecurities.add(latest);
    });

    persistanceProvider.updateSecurities(persistedSecurities);
  }

  private static boolean isBetween(double threshold, double a1, double a2) {
    return threshold >= Math.min(a1, a2) && threshold <= Math.max(a1, a2);
  }

  private void checkAndRaiseAlert(final SecurityConfig config, final Security latest, final Security persisted) {
    if (latest==null || persisted==null) {
      log.warn("Cannot check alert requirement since either latest[{}] or persisted[{}] value is empty.", latest, persisted);
      return;
    }

    // TODO: consider silence mode

    config.getAlerts().stream()
        .filter(alert -> isBetween(alert.getThreshold(), latest.price(), persisted.price()))
        .peek(alert -> log.debug("Send alert for {}", alert))
        .forEach(alert -> notificationService.send(alert, latest, persisted));
  }

}
