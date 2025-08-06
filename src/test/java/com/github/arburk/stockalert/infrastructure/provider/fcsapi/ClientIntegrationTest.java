package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.service.stock.StockProvider;
import com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto.StockApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestPropertySource(locations = "classpath:application-test.yml")
class ClientIntegrationTest {

  @MockitoBean
  private StockClient stockClient;

  @Autowired
  private StockProvider fcsapiClient;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void performRealApiCall() {
    assertNotNull(fcsapiClient);
    when(stockClient.getLatestStocks(anyString(),anyString())).thenReturn(getMockedReponse());

    final Collection<Security> result = fcsapiClient.getLatest(List.of("IBM,AMD,AAPL,MSFT,FB,TSLA"));

    assertNotNull(result);
    assertEquals(5, result.size());

    assertTrue(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("AMD")));
    assertTrue(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("AAPL")));
    assertTrue(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("MSFT")));

    assertFalse(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("IBM")));
    assertFalse(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("FB")));
    assertFalse(result.stream().map(Security::symbol).anyMatch(symbol -> symbol.equals("TSLA")));
  }

  private StockApiResponse getMockedReponse() {
    try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("rest-client/result-200-demo_api.json")) {
      return objectMapper.readValue(inputStream, StockApiResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}