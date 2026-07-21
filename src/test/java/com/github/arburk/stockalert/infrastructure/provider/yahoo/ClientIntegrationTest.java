package com.github.arburk.stockalert.infrastructure.provider.yahoo;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.stock.StockProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestPropertySource(locations = "classpath:application-test.yml")
class ClientIntegrationTest {

  @MockitoBean
  private YahooFinanceClient yahooFinanceClient;

  @Autowired
  private StockProvider yahooClient;

  @Test
  void performMockedApiCall() {
    assertNotNull(yahooClient);
    when(yahooFinanceClient.getChart(eq("NESN.SW"), anyString(), anyString()))
        .thenReturn(ClientTest.getChartResponse("chart-NESN.SW.json"));
    // "UNKNOWN" is not mocked -> null response -> must be skipped without breaking the batch

    List<SecurityConfig> securities = new ArrayList<>();
    securities.add(new SecurityConfig("NESN.SW", "Switzerland", null, null, null, null));
    securities.add(new SecurityConfig("UNKNOWN", null, null, null, null, null));
    final Collection<Security> result = yahooClient.getLatest(securities);

    assertNotNull(result);
    assertEquals(1, result.size());
    final Security nesn = result.iterator().next();
    assertEquals("NESN.SW", nesn.symbol());
    assertEquals(78.5, nesn.price());
    assertEquals("CHF", nesn.currency());
    assertEquals("Switzerland", nesn.exchange(), "exchange is expected to be stamped from config-example.json");
    assertTrue(result.stream().map(Security::symbol).noneMatch("UNKNOWN"::equals));
    assertFalse(result.isEmpty());
  }

}
