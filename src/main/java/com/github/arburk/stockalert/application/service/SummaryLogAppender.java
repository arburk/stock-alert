package com.github.arburk.stockalert.application.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.github.arburk.stockalert.application.service.RunSummary.LogProblem;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logback appender that records every {@code WARN}/{@code ERROR} event emitted during a run into
 * {@link RunSummary}, so the generated Markdown summary can list problems from any component
 * (fetch, notification, persistence, scheduler) without the workflow having to grep the raw log.
 *
 * <p>The root logger is a JVM-global singleton shared across all Spring contexts, so attachment is
 * kept idempotent (a single appender registered under {@link #APPENDER_NAME}) and is detached again
 * on context shutdown. This prevents appenders from leaking/accumulating across the many cached
 * application contexts a test run spins up in one JVM. There is no logback XML config in this
 * project, hence the programmatic wiring.
 */
@Component
public class SummaryLogAppender extends AppenderBase<ILoggingEvent> {

  static final String APPENDER_NAME = "runSummaryAppender";

  private final RunSummary runSummary;

  public SummaryLogAppender(final RunSummary runSummary) {
    this.runSummary = runSummary;
  }

  @PostConstruct
  void attach() {
    final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    setName(APPENDER_NAME);
    setContext(root.getLoggerContext());
    // drop any stale instance left attached by a previously started (e.g. cached test) context
    root.detachAppender(APPENDER_NAME);
    start();
    root.addAppender(this);
  }

  @PreDestroy
  void detach() {
    final Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    root.detachAppender(this);
    stop();
  }

  @Override
  protected void append(final ILoggingEvent event) {
    if (event.getLevel().isGreaterOrEqual(Level.WARN)) {
      runSummary.addProblem(new LogProblem(
          event.getLevel().toString(),
          event.getLoggerName(),
          event.getFormattedMessage()));
    }
  }
}
