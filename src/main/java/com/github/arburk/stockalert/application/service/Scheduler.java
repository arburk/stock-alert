package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.service.stock.StockService;
import io.micrometer.common.util.StringUtils;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Scheduler {

  private final StockService stockService;
  private final RunSummary runSummary;
  private final ApplicationConfig applicationConfig;

  public Scheduler(final StockService stockService, final RunSummary runSummary, final ApplicationConfig applicationConfig) {
    this.stockService = stockService;
    this.runSummary = runSummary;
    this.applicationConfig = applicationConfig;
  }

  @Scheduled(cron = "${stock-alert.update-cron}")
  public void updateStock() {
    log.info("Updating stock...");
    runSummary.reset();
    try {
      stockService.update();
    } catch (Exception e) {
      log.error("failed to update stock data: {}", e.getMessage());
    }
    writeRunSummary();
    log.info("Stock update finished.");
  }

  private void writeRunSummary() {
    final String summaryFile = applicationConfig.getSummaryFile();
    if (StringUtils.isBlank(summaryFile)) {
      return;
    }
    runSummary.writeTo(Path.of(summaryFile));
  }
}
