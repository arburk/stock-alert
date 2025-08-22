package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
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
    log.debug("refresh stock alert config...");
    final List<SecurityConfig> alertConfig = this.applicationConfig.getStockAlertsConfig().securities();
    final Set<String> symbolsToQueryFor = alertConfig.stream().map(SecurityConfig::symbol).collect(Collectors.toSet());
    log.debug("perform and process update for {}", symbolsToQueryFor);
    try {
      process(alertConfig, stockProvider.getLatest(symbolsToQueryFor));
    } catch (Exception e) {
      log.error("update did not finish successful: {}", e.getMessage(), e);
    }
  }

  private void process(final List<SecurityConfig> alertConfig, final Collection<Security> latestSecurities) {
    final Collection<Security> latestRelevant = getRelevantFiltered(alertConfig, latestSecurities);
    final Collection<Security> persistedSecurites = persistanceProvider.getSecurites();
    alertConfig.forEach(configElement -> checkAndRaiseAlert(
        configElement,
        getSecurity(latestRelevant, configElement),
        getSecurity(persistedSecurites, configElement)
    ));
    updatePersistedSecurities(persistedSecurites, latestRelevant);
  }

  private Collection<Security> getRelevantFiltered(final List<SecurityConfig> alertConfig, final Collection<Security> latestSecurities) {
    final Set<String> configKeys = alertConfig.stream()
        .map(cfg -> cfg.symbol() + "::" + cfg.exchange())
        .collect(Collectors.toSet());
    final List<Security> filteredSecurites = latestSecurities.stream()
        .filter(sec -> configKeys.contains(sec.symbol() + "::" + sec.exchange()))
        .toList();

    if (configKeys.size() != filteredSecurites.size()) {
      logUnidentifiedSecurities(latestSecurities, configKeys);
      //TODO: send warning to check config?
    }

    return filteredSecurites;
  }

  private static void logUnidentifiedSecurities(final Collection<Security> latestSecurities, final Set<String> configKeys) {
    final Set<String> found = latestSecurities.stream()
        .map(sec -> sec.symbol() + "::" + sec.exchange())
        .collect(Collectors.toSet());
    final List<String> unmatched = configKeys.stream().filter(entry -> !found.contains(entry)).toList();
    log.warn("Did not find following stocks configured in alert config: {}", unmatched);
  }

  private Security getSecurity(final Collection<Security> securities, final SecurityConfig securityConfig) {
    if (securities == null || securities.isEmpty()) {
      return null;
    }

    return securities.stream().filter(security ->
            security.symbol().equals(securityConfig.symbol())
                && security.exchange().equals(securityConfig.exchange()))
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

    final List<Alert> alerts = config.alerts();
    if (alerts == null || alerts.isEmpty()) {
      log.debug("Skip further checks since no alert defined for {}", latest.symbol());
      return;
    }

    alerts.stream()
        .filter(alert -> isBetween(alert.threshold(), latest.price(), persisted.price()))
        .forEach(alert -> {
          log.info("Send alert for {} {}", latest.symbol(), alert);
          notificationService.send(alert, latest, persisted);
        });
  }

}
