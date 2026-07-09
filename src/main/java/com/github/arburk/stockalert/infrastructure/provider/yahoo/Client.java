package com.github.arburk.stockalert.infrastructure.provider.yahoo;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.stock.StockProvider;
import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.Chart;
import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.ChartResponse;
import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.Meta;
import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.SecurityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Client implements StockProvider {

  static final String INTERVAL = "1d";
  static final String RANGE = "1d";

  private final YahooFinanceClient yahooFinanceClient;
  private final ApplicationConfig applicationConfig;

  public Client(final YahooFinanceClient yahooFinanceClient, final ApplicationConfig applicationConfig) {
    this.yahooFinanceClient = yahooFinanceClient;
    this.applicationConfig = applicationConfig;
    log.info("YahooFinanceClient instance created: {}", yahooFinanceClient);
  }

  @Override
  public Collection<Security> getLatest(final Collection<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      log.warn("No symbols provided");
      return Collections.emptyList();
    }

    final Map<String, String> exchangeBySymbol = getExchangeBySymbol();

    return symbols.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(symbol -> !symbol.isEmpty())
        .distinct()
        .map(symbol -> fetchQuote(symbol, exchangeBySymbol))
        .flatMap(Optional::stream)
        .collect(Collectors.toSet());
  }

  /**
   * Yahoo's exchange names (e.g. "EBS", "GER") do not match the exchange labels used in the
   * alert config (e.g. "Switzerland", "Xetra"). The exchange from the config is stamped on
   * each result so symbol/exchange matching downstream keeps working unchanged.
   */
  private Map<String, String> getExchangeBySymbol() {
    final List<SecurityConfig> securities = applicationConfig.getStockAlertsConfig().securities();
    if (securities == null || securities.isEmpty()) {
      return Collections.emptyMap();
    }
    return securities.stream()
        .filter(config -> config.symbol() != null && config.exchange() != null)
        .collect(Collectors.toMap(SecurityConfig::symbol, SecurityConfig::exchange, (first, second) -> {
          if (!first.equals(second)) {
            log.warn("Duplicate symbol with different exchanges in config ('{}' vs '{}'): using '{}'", first, second, first);
          }
          return first;
        }));
  }

  private Optional<Security> fetchQuote(final String symbol, final Map<String, String> exchangeBySymbol) {
    try {
      final ChartResponse response = yahooFinanceClient.getChart(symbol, INTERVAL, RANGE);
      final Chart chart = response == null ? null : response.chart();
      if (chart == null) {
        log.error("Empty response for symbol {}: skipped", symbol);
        return Optional.empty();
      }
      if (chart.error() != null) {
        log.error("API error for symbol {}: {} - {}", symbol, chart.error().code(), chart.error().description());
        return Optional.empty();
      }
      if (chart.result() == null || chart.result().isEmpty() || chart.result().getFirst().meta() == null) {
        log.error("No result data for symbol {}: skipped", symbol);
        return Optional.empty();
      }
      final Meta meta = chart.result().getFirst().meta();
      log.debug("{}: {} {} (previous close {})", meta.symbol(), meta.regularMarketPrice(), meta.currency(), meta.chartPreviousClose());
      return Optional.of(SecurityMapper.INSTANCE.fromChartMeta(meta, resolveExchange(symbol, meta, exchangeBySymbol)));
    } catch (Exception e) {
      log.error("Failed to fetch quote for symbol {}: {}", symbol, e.getMessage());
      return Optional.empty();
    }
  }

  private String resolveExchange(final String symbol, final Meta meta, final Map<String, String> exchangeBySymbol) {
    final String configured = exchangeBySymbol.get(symbol);
    if (configured != null) {
      return configured;
    }
    log.warn("Symbol {} not found in alert config: falling back to Yahoo exchange name '{}'", symbol, meta.fullExchangeName());
    return meta.fullExchangeName();
  }

}
