package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.service.stock.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Scheduler {

  private final StockService stockService;

  public Scheduler(final StockService stockService) {
    this.stockService = stockService;
  }

  @Scheduled(cron = "${stock-alert.update-cron}")
  public void updateStock() {
    log.info("Updating stock...");
    try {
      stockService.update();
    } catch (Exception e) {
      log.error("failed to update stock data: {}", e.getMessage());
    }
  }
}
