package com.github.arburk.stockalert.application.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.service.stock.StockService;
import com.github.arburk.stockalert.application.service.stock.StorageException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

  @Test
  void successfulRun_logsFinishedMarker() {
    final List<String> messages = captureSchedulerLog(() -> new Scheduler(stockService, runSummary, applicationConfig).updateStock());

    assertTrue(messages.contains("Stock update finished."));
    assertFalse(messages.contains("Stock update failed."));
  }

  @Test
  void failedRun_logsFailedMarkerInsteadOfFinished() {
    doThrow(new StorageException("S3 not reachable", new RuntimeException())).when(stockService).update();

    final List<String> messages = captureSchedulerLog(() -> new Scheduler(stockService, runSummary, applicationConfig).updateStock());

    assertTrue(messages.contains("Stock update failed."));
    assertFalse(messages.stream().anyMatch(m -> m.contains("Stock update finished.")),
        "workflow treats 'Stock update finished.' as success marker");
  }

  private static List<String> captureSchedulerLog(final Runnable action) {
    final Logger logger = (Logger) LoggerFactory.getLogger(Scheduler.class);
    final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      action.run();
    } finally {
      logger.detachAppender(appender);
    }
    return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
  }
}