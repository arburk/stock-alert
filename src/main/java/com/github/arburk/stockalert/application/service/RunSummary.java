package com.github.arburk.stockalert.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Collects a single run's per-ticker outcomes and any WARN/ERROR log events, then renders them as
 * a GitHub-flavoured Markdown report ({@link #renderMarkdown()} / {@link #writeTo(Path)}) that the
 * scheduled GitHub Actions workflow appends to the job summary.
 *
 * <p>A single run is bracketed by {@link #reset()} (start) and {@link #writeTo(Path)} (end).
 * Per-ticker data is fed in by the Yahoo client; WARN/ERROR events are fed in by
 * {@link SummaryLogAppender}.
 */
@Slf4j
@Component
public class RunSummary {

  private static final DateTimeFormatter RUN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  /** One queried ticker. {@code httpStatus == null} renders as {@code ERR} (no HTTP response). */
  public record TickerResult(
      String symbol,
      Integer httpStatus,
      boolean success,
      Double price,
      String currency,
      String priceDate,
      String note) {
  }

  /** One captured WARN/ERROR log event. */
  public record LogProblem(String level, String logger, String message) {
  }

  private List<TickerResult> tickers = Collections.synchronizedList(new ArrayList<>());
  private List<LogProblem> problems = Collections.synchronizedList(new ArrayList<>());

  /** Clears all collected data at the start of a run. */
  public void reset() {
    tickers = Collections.synchronizedList(new ArrayList<>());
    problems = Collections.synchronizedList(new ArrayList<>());
  }

  public void addTicker(final TickerResult result) {
    tickers.add(result);
  }

  public void addProblem(final LogProblem problem) {
    problems.add(problem);
  }

  public List<TickerResult> getTickers() {
    return new ArrayList<>(tickers);
  }

  public List<LogProblem> getProblems() {
    return new ArrayList<>(problems);
  }

  public void writeTo(final Path target) {
    try {
      Files.writeString(target, renderMarkdown());
      log.info("Run summary written to {}", target);
    } catch (IOException e) {
      log.error("Failed to write run summary to {}: {}", target, e.getMessage());
    }
  }

  public String renderMarkdown() {
    final StringBuilder md = new StringBuilder();
    md.append("## 📈 Stock update summary\n\n");
    md.append("_").append(LocalDateTime.now().format(RUN_FORMATTER)).append("_\n\n");
    renderTable(md);
    md.append('\n');
    renderProblems(md);
    return md.toString();
  }

  private void renderTable(final StringBuilder md) {
    md.append("| Symbol | HTTP | Price | Date |\n");
    md.append("|---|---|---|---|\n");
    final List<TickerResult> sorted = new ArrayList<>(tickers);
    sorted.sort(Comparator.comparing(t -> t.symbol() == null ? "" : t.symbol(), String.CASE_INSENSITIVE_ORDER));
    if (sorted.isEmpty()) {
      md.append("| _no tickers queried_ |  |  |  |\n");
      return;
    }
    for (final TickerResult t : sorted) {
      md.append("| ").append(cell(t.symbol()))
          .append(" | ").append(statusCell(t))
          .append(" | ").append(priceCell(t))
          .append(" | ").append(t.success() ? cell(t.priceDate()) : "–")
          .append(" |\n");
    }
  }

  private static String statusCell(final TickerResult t) {
    final String icon = t.success() ? "✅" : "❌";
    final String code = t.httpStatus() == null ? "ERR" : String.valueOf(t.httpStatus());
    return icon + " " + code;
  }

  private static String priceCell(final TickerResult t) {
    if (t.success() && t.price() != null) {
      final String value = String.format(Locale.ROOT, "%.2f", t.price());
      return t.currency() == null ? value : value + " " + t.currency();
    }
    return t.note() == null || t.note().isBlank() ? "–" : "_" + cell(t.note()) + "_";
  }

  /** Escapes Markdown-table-breaking characters ({@code |} and newlines). */
  private static String cell(final String raw) {
    if (raw == null || raw.isBlank()) {
      return "–";
    }
    return raw.replace("|", "\\|").replace("\n", " ").replace("\r", " ").trim();
  }

  private void renderProblems(final StringBuilder md) {
    final List<LogProblem> snapshot = new ArrayList<>(problems);
    if (snapshot.isEmpty()) {
      md.append("> [!NOTE]\n> No warnings or errors during this run.\n");
      return;
    }
    final long warnings = snapshot.stream().filter(p -> "WARN".equalsIgnoreCase(p.level())).count();
    final long errors = snapshot.size() - warnings;
    md.append("<details>\n<summary>⚠️ ")
        .append(errors).append(" error(s), ").append(warnings).append(" warning(s)")
        .append("</summary>\n\n");
    for (final LogProblem p : snapshot) {
      final String kind = "WARN".equalsIgnoreCase(p.level()) ? "WARNING" : "CAUTION";
      final String message = p.message() == null ? "" : p.message().replace("\n", "\n> ").replace("\r", "");
      md.append("> [!").append(kind).append("]\n> `").append(shortLogger(p.logger())).append("` — ")
          .append(message).append("\n\n");
    }
    md.append("</details>\n");
  }

  private static String shortLogger(final String logger) {
    if (logger == null || logger.isBlank()) {
      return "?";
    }
    final int dot = logger.lastIndexOf('.');
    return dot < 0 ? logger : logger.substring(dot + 1);
  }
}
