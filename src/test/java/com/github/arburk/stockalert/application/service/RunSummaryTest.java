package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.service.RunSummary.LogProblem;
import com.github.arburk.stockalert.application.service.RunSummary.TickerResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunSummaryTest {

  @Test
  void rendersTableWithIconsPricesAndProblems() {
    final RunSummary summary = new RunSummary();
    summary.addTicker(new TickerResult("NESN.SW", 200, true, 84.2, "CHF", "2026-07-24 17:30", null));
    summary.addTicker(new TickerResult("BADSYM", 404, false, null, null, null, "Not Found: No data found"));
    summary.addTicker(new TickerResult("TIMEOUT", null, false, null, null, null, "connect timed out"));
    summary.addProblem(new LogProblem("WARN",
        "com.github.arburk.stockalert.application.service.stock.StockService", "Did not find configured stocks"));
    summary.addProblem(new LogProblem("ERROR", "com.github.arburk.x.Client", "boom"));

    final String md = summary.renderMarkdown();

    assertTrue(md.contains("| Symbol | HTTP | Price | Date |"), md);
    // success row: green icon, formatted price with currency, price date
    assertTrue(md.contains("✅ 200"), md);
    assertTrue(md.contains("84.20 CHF"), md);
    assertTrue(md.contains("2026-07-24 17:30"), md);
    // HTTP failure row: red icon with real code, reason rendered in place of price
    assertTrue(md.contains("❌ 404"), md);
    assertTrue(md.contains("_Not Found: No data found_"), md);
    // non-HTTP failure row: ERR placeholder
    assertTrue(md.contains("❌ ERR"), md);
    // rows are sorted alphabetically (BADSYM before NESN.SW before TIMEOUT)
    assertTrue(md.indexOf("BADSYM") < md.indexOf("NESN.SW"), md);

    // problems section
    assertTrue(md.contains("<details>"), md);
    assertTrue(md.contains("1 error(s), 1 warning(s)"), md);
    assertTrue(md.contains("> [!WARNING]"), md);
    assertTrue(md.contains("> [!CAUTION]"), md);
    assertTrue(md.contains("`StockService`"), md);
  }

  @Test
  void rendersEmptyRunGracefully() {
    final String md = new RunSummary().renderMarkdown();

    assertTrue(md.contains("_no tickers queried_"), md);
    assertTrue(md.contains("> [!NOTE]"), md);
    assertFalse(md.contains("<details>"), md);
  }

  @Test
  void resetClearsCollectedData() {
    final RunSummary summary = new RunSummary();
    summary.addTicker(new TickerResult("NESN.SW", 200, true, 84.2, "CHF", "2026-07-24 17:30", null));
    summary.addProblem(new LogProblem("WARN", "x", "y"));

    summary.reset();

    assertTrue(summary.getTickers().isEmpty());
    assertTrue(summary.getProblems().isEmpty());
  }
}
