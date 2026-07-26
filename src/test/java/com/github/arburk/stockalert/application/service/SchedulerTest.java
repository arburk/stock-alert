package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.service.stock.StockService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SchedulerTest {

  final StockService stockService = Mockito.mock(StockService.class);
  final RunSummary runSummary = Mockito.mock(RunSummary.class);
  final ApplicationConfig applicationConfig = Mockito.mock(ApplicationConfig.class);

  @Test
  void testInjection() {
    Scheduler testee = new Scheduler(stockService, runSummary, applicationConfig);
    assertNotNull(testee);
    testee.updateStock();
    verify(stockService, times(1)).update();
    verify(runSummary, times(1)).reset();
  }

  @Test
  void verifyExceptionsDoNotBreak() {
    doThrow(new RuntimeException("Test Exception")).when(stockService).update();
    assertDoesNotThrow(() -> new Scheduler(stockService, runSummary, applicationConfig).updateStock());
  }
}