package com.github.arburk.stockalert.application.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SchedulerTest {

  final StockService stockService = Mockito.mock(StockService.class);

  @Test
  void testInjection() {
    Scheduler testee = new Scheduler(stockService);
    assertNotNull(testee);
    testee.updateStock();
    verify(stockService, times(1)).update();
  }

  @Test
  void verifyExceptionsDoNotBreak() {
    doThrow(new RuntimeException("Test Exception")).when(stockService).update();
    assertDoesNotThrow(() -> new Scheduler(stockService).updateStock());
  }
}