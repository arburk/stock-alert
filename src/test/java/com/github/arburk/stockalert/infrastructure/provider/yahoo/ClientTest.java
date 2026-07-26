package com.github.arburk.stockalert.infrastructure.provider.yahoo;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.service.RunSummary;
import com.github.arburk.stockalert.application.service.RunSummary.TickerResult;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.infrastructure.provider.yahoo.dto.ChartResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientTest {

  private YahooFinanceClient yahooFinanceClient;
  private ApplicationConfig applicationConfig;
  private RunSummary runSummary;
  private Client testee;

  @BeforeEach
  void setUp() {
    yahooFinanceClient = Mockito.mock(YahooFinanceClient.class);
    applicationConfig = Mockito.mock(ApplicationConfig.class);
    when(applicationConfig.getStockAlertsConfig()).thenReturn(configWith(
        new SecurityConfig("BALN.SW", "Switzerland", null, null, null, null),
        new SecurityConfig("INGA.AS", "Amsterdam", null, null, null, null),
        new SecurityConfig("MMM", "NYSE", null, null, null, null)
    ));
    runSummary = new RunSummary();
    testee = new Client(yahooFinanceClient, runSummary);
  }

  private TickerResult resultFor(final String symbol) {
    return runSummary.getTickers().stream()
        .filter(t -> symbol.equals(t.symbol()))
        .findFirst().orElseThrow();
  }

  @Test
  void happyMappingFlow_ExchangeStampedFromConfig() {
    when(yahooFinanceClient.getChart(eq("BALN.SW"), anyString(), anyString())).thenReturn(getChartResponse("chart-BALN.SW.json"));
    when(yahooFinanceClient.getChart(eq("INGA.AS"), anyString(), anyString())).thenReturn(getChartResponse("chart-INGA.AS.json"));

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("BALN.SW", "Switzerland", null, null, null, null));
    securities.add(new SecurityConfig("INGA.AS", null, null, null, null, null));
    final Collection<Security> result = testee.getLatest(securities);

    assertEquals(2, result.size());
    final Security baln = result.stream().filter(sec -> "BALN.SW".equals(sec.symbol())).findFirst().orElseThrow();
    assertEquals(207.4, baln.price());
    assertEquals("CHF", baln.currency());
    assertEquals("Switzerland", baln.exchange(), "exchange is expected to be stamped from config, not from Yahoo's exchange name");
    final Security inga = result.stream().filter(sec -> "INGA.AS".equals(sec.symbol())).findFirst().orElseThrow();
    assertEquals("Amsterdam", inga.exchange());
  }

  @Test
  void assertDuplicatesAndBlanksRemovedFromQuery() {
    when(yahooFinanceClient.getChart(eq("BALN.SW"), anyString(), anyString())).thenReturn(getChartResponse("chart-BALN.SW.json"));
    when(yahooFinanceClient.getChart(eq("INGA.AS"), anyString(), anyString())).thenReturn(getChartResponse("chart-INGA.AS.json"));

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("BALN.SW", "Switzerland", null, null, null, null));
    securities.add(new SecurityConfig("INGA.AS", null, null, null, null, null));
    final Collection<Security> result = testee.getLatest(securities);

    assertEquals(2, result.size());
    verify(yahooFinanceClient, times(1)).getChart("BALN.SW", Client.INTERVAL, Client.RANGE);
    verify(yahooFinanceClient, times(1)).getChart("INGA.AS", Client.INTERVAL, Client.RANGE);
  }

  @Test
  void failingSymbolIsSkippedWhileBatchContinues() {
    when(yahooFinanceClient.getChart(eq("BALN.SW"), anyString(), anyString())).thenReturn(getChartResponse("chart-BALN.SW.json"));
    when(yahooFinanceClient.getChart(eq("BROKEN"), anyString(), anyString())).thenThrow(new RuntimeException("HTTP 404 simulated"));
    when(yahooFinanceClient.getChart(eq("INGA.AS"), anyString(), anyString())).thenReturn(getChartResponse("chart-INGA.AS.json"));

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("BALN.SW", "Switzerland", null, null, null, null));
    securities.add(new SecurityConfig("BROKEN", null, null, null, null, null));
    securities.add(new SecurityConfig("INGA.AS", null, null, null, null, null));
    final Collection<Security> result = testee.getLatest(securities);

    assertEquals(2, result.size());
    assertTrue(result.stream().map(Security::symbol).noneMatch("BROKEN"::equals));

    // every queried symbol is recorded in the run summary, including the failed one
    assertEquals(3, runSummary.getTickers().size());
    final TickerResult baln = resultFor("BALN.SW");
    assertTrue(baln.success());
    assertEquals(200, baln.httpStatus());
    assertEquals(207.4, baln.price());
    final TickerResult broken = resultFor("BROKEN");
    assertFalse(broken.success());
    assertNull(broken.httpStatus(), "non-Feign exception carries no HTTP status");
    assertTrue(broken.note().contains("404"));
  }

  @Test
  void errorNodeInResponseIsSkipped() {
    when(yahooFinanceClient.getChart(eq("DELISTED"), anyString(), anyString())).thenReturn(getChartResponse("chart-error-not-found.json"));
    when(yahooFinanceClient.getChart(eq("BALN.SW"), anyString(), anyString())).thenReturn(getChartResponse("chart-BALN.SW.json"));

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("BALN.SW", "Switzerland", null, null, null, null));
    securities.add(new SecurityConfig("DELISTED", null, null, null, null, null));
    final Collection<Security> result = testee.getLatest(securities);

    assertEquals(1, result.size());
    assertEquals("BALN.SW", result.iterator().next().symbol());

    final TickerResult delisted = resultFor("DELISTED");
    assertFalse(delisted.success());
    assertEquals(200, delisted.httpStatus(), "Yahoo error node still comes back over HTTP 200");
  }

  @Test
  void exchangeFallsBackToYahooNameIfSymbolNotInConfig() {
    when(yahooFinanceClient.getChart(eq("ROG.SW"), anyString(), anyString())).thenReturn(getChartResponse("chart-ROG.SW.json"));

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("ROG.SW", null, null, null, null, null));
    final Collection<Security> result = testee.getLatest(securities);

    assertEquals(1, result.size());
    assertEquals("Swiss Exchange", result.iterator().next().exchange(), "expected fallback to Yahoo's fullExchangeName");
  }

  @Test
  void skipEmptyRequest() {
    assertTrue(testee.getLatest(new ArrayList<>()).isEmpty());
    assertTrue(testee.getLatest(null).isEmpty());
    verify(yahooFinanceClient, never()).getChart(anyString(), anyString(), anyString());
    verify(applicationConfig, never()).getStockAlertsConfig();
  }

  private static StockAlertsConfig configWith(SecurityConfig... securities) {
    return new StockAlertsConfig(null, null, null, null, List.of(securities));
  }

  static ChartResponse getChartResponse(final String fixture) {
    try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("rest-client/yahoo/" + fixture)) {
      return new JacksonConfig().objectMapper().readValue(inputStream, ChartResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
