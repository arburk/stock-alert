package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.application.service.notification.NotificationService;
import lombok.NonNull;
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
  final PersistenceProvider persistenceProvider;
  final NotificationService notificationService;

  public StockService(ApplicationConfig applicationConfig, StockProvider stockProvider, PersistenceProvider persistenceProvider, NotificationService notificationService) {
    this.stockProvider = stockProvider;
    this.applicationConfig = applicationConfig;
    this.persistenceProvider = persistenceProvider;
    this.notificationService = notificationService;
  }

  public void update() {
    log.debug("refresh stock alert config...");
    final StockAlertsConfig stockAlertsConfig = this.applicationConfig.getStockAlertsConfig();
    final List<SecurityConfig> alertConfig = stockAlertsConfig.securities();
    final Set<String> symbolsToQueryFor = alertConfig.stream().map(SecurityConfig::symbol).collect(Collectors.toSet());
    log.debug("perform and process update for {}", symbolsToQueryFor);

    try {
      final Collection<Security> latestRelevant = getRelevantFiltered(alertConfig, stockProvider.getLatest(symbolsToQueryFor));
      final Collection<Security> persistedSecurites = persistenceProvider.getSecurites();
      alertConfig.forEach(configElement -> checkSecurityAndRaiseAlert(
          stockAlertsConfig, configElement,
          getSecurity(latestRelevant, configElement),
          getSecurity(persistedSecurites, configElement)
      ));

      updatePersistedSecurities(persistedSecurites, latestRelevant);
    } catch (Exception e) {
      log.error("update did not finish successful: {}", e.getMessage(), e);
    }
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

    persistenceProvider.updateSecurities(persistedSecurities);
  }

  private static boolean isBetween(double threshold, double a1, double a2) {
    return threshold >= Math.min(a1, a2) && threshold <= Math.max(a1, a2);
  }

  private void checkSecurityAndRaiseAlert(final StockAlertsConfig stockAlertsConfig, final SecurityConfig securityConfig, final Security latest, final Security persisted) {
    if (latest == null) {
      log.warn("Cannot check alert requirement since latest value is empty. Check configuration for proper security settings.");
      return;
    }

    if (persisted == null) {
      log.info("Cannot check alert requirement since persisted value is empty.");
      return;
    }

    final List<Alert> alerts = securityConfig.alerts();
    if (alerts != null && !alerts.isEmpty()) {
      alerts.stream()
          .filter(alert -> isBetween(alert.threshold(), latest.price(), persisted.price()))
          .forEach(alert -> {
            log.info("Send alert for {} {}", latest.symbol(), alert);
            notificationService.send(stockAlertsConfig, alert, latest, persisted);
          });
    }

    checkAndRaisePercentageAlert(stockAlertsConfig, securityConfig, latest, persisted);
  }

  private void checkAndRaisePercentageAlert(
      @NonNull final StockAlertsConfig stockAlertsConfig,
      @NonNull final SecurityConfig config,
      @NonNull final Security latest,
      @NonNull final Security persisted) {

    final Double percentageAlert = stockAlertsConfig.getPercentageAlert();
    var globalDef = (percentageAlert != null && percentageAlert > 0)
        ? percentageAlert
        : null;
    var overrideDef = (config.getPercentageAlert() != null) ? config.getPercentageAlert() : null;
    var threshold2consider = overrideDef != null ? overrideDef : globalDef;
    if (threshold2consider == null || threshold2consider == 0) {
      log.debug("skip percentage alert for {}", latest.symbol());
      return;
    }

    final double cpProvided = latest.changePercentage() != null ? latest.changePercentage() : 0;

    final Double latestPrice = latest.price();
    final Double persistedPrice = persisted.price();
    double cpCalculated = (latestPrice / persistedPrice) - 1;

    double cpBiggest = Math.abs(cpProvided) > Math.abs(cpCalculated) ? cpProvided : cpCalculated;

    if (Math.abs(cpBiggest) >= threshold2consider) {
      log.debug("Percentage deviation calculated {} / provided {} > {} -> raise alert for {}!", cpCalculated, cpProvided, threshold2consider, latest.symbol());
      notificationService.sendPercentage(stockAlertsConfig, latest, persisted, threshold2consider, cpBiggest);
    }
  }

}
