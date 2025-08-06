package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto.StockApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientTest {

  private StockClient stockClient;
  private ApplicationConfig applicationConfig;
  private Client testee;
  private static final StockApiResponse result200 = ClientTest.getResult200();

  @BeforeEach
  void setUp() {
    stockClient = Mockito.mock(StockClient.class);
    applicationConfig = Mockito.mock(ApplicationConfig.class);
    testee = new Client(stockClient, applicationConfig);
  }

  @Test
  void happyMappingFlow() {
    final ArgumentCaptor<String> symbolsCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    when(stockClient.getLatestStocks(symbolsCaptor.capture(), keyCaptor.capture())).thenReturn(result200);
    when(applicationConfig.getFcsApiKey()).thenReturn("myApiKeyToVerify");

    final Collection<Security> result = testee.getLatest(List.of("MRK", "INGA", "BALN", "ROG", "PFE"));

    assertEquals("myApiKeyToVerify", keyCaptor.getValue());
    assertEquals("MRK,INGA,BALN,ROG,PFE", symbolsCaptor.getValue());
    assertEquals(66, result.size());
  }

  @Test
  void assertDuplicatesRemovedFromQuery() {
    final ArgumentCaptor<String> symbolsCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    when(stockClient.getLatestStocks(symbolsCaptor.capture(), keyCaptor.capture())).thenReturn(result200);
    when(applicationConfig.getFcsApiKey()).thenReturn("myApiKeyToVerify");

    final Collection<Security> result = testee.getLatest(List.of("MRK", "INGA", "BALN", "INGA ", "BALN", "ROG", "BALN", "DEVN"));

    assertEquals("myApiKeyToVerify", keyCaptor.getValue());
    assertEquals("MRK,INGA,BALN,ROG,DEVN", symbolsCaptor.getValue());
    assertEquals(66, result.size());
  }

  @Test
  void skipEmptyRequest() {
    final ArgumentCaptor<String> symbolsCaptor = ArgumentCaptor.forClass(String.class);
    final ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    when(stockClient.getLatestStocks(symbolsCaptor.capture(), keyCaptor.capture())).thenReturn(result200);
    when(applicationConfig.getFcsApiKey()).thenReturn("myApiKeyToVerify");

    final Collection<Security> result = testee.getLatest(Collections.emptyList());

    assertTrue(result.isEmpty());
    verify(stockClient, never()).getLatestStocks(anyString(), anyString());
    verify(applicationConfig, never()).getFcsApiKey();
  }

  private static StockApiResponse getResult200() {
    try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("rest-client/result-200-66.json")) {
      return new ObjectMapper().readValue(inputStream, StockApiResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}