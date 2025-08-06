package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

  private ApplicationConfig applicationConfig;
  private StockProvider stockProvider;
  private PersistanceProvider persistanceProvider;

  private StockService testee;

  @BeforeEach
  void setUp() {
    applicationConfig = new ApplicationConfig(new JacksonConfig().objectMapper());
    applicationConfig.setConfigUrl(Path.of("src/main/resources/config-example.json").toUri().toString());
    stockProvider = Mockito.mock(StockProvider.class);
    persistanceProvider = Mockito.mock(PersistanceProvider.class);
    testee = new StockService(stockProvider, applicationConfig, persistanceProvider);
  }

  @Test
  void updateHappyFlow() {
    final ArgumentCaptor<Collection<String>> stringCollection = ArgumentCaptor.forClass(Collection.class);
    final ArgumentCaptor<Collection<Security>> securityCollection = ArgumentCaptor.forClass(Collection.class);
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(getSecurites(true, updatedTs));
    when(persistanceProvider.getSecurites()).thenReturn(getSecurites(true, oldTs));

    testee.update();

    verify(persistanceProvider).updateSecurities(securityCollection.capture());
    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(1, latestRequest.size());
    assertEquals("[BALN]", latestRequest.toString());
    final Collection<Security> updated = securityCollection.getValue();
    assertEquals(1, updated.size());
    final Security first = updated.stream().toList().getFirst();
    assertEquals("BALN", first.symbol());
    assertEquals(updatedTs, first.timestamp());
  }

  @Test
  void update_NoInput() {
    final ArgumentCaptor<Collection<String>> stringCollection = ArgumentCaptor.forClass(Collection.class);
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(getSecurites(false, updatedTs));
    when(persistanceProvider.getSecurites()).thenReturn(getSecurites(true, oldTs));

    testee.update();

    verify(persistanceProvider, never()).updateSecurities(any());
    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(1, latestRequest.size());
    assertEquals("[BALN]", latestRequest.toString());
  }

  @Test
  void updateIncomplete() {
    applicationConfig.setConfigUrl(Path.of("src/test/resources/config/config-test.json").toUri().toString());
    final ArgumentCaptor<Collection<String>> stringCollection = ArgumentCaptor.forClass(Collection.class);
    final ArgumentCaptor<Collection<Security>> securityCollection = ArgumentCaptor.forClass(Collection.class);
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);

    final Collection<Security> providedSecurities = getSecurites(false, updatedTs);
    providedSecurities.add(new Security("HELN", 176.25, "CHF", updatedTs, "Switzerland"));
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(providedSecurities);

    final Collection<Security> securitiesPersisted = getSecurites(true, oldTs);
    securitiesPersisted.add(new Security("HELN", 176.25, "CHF", oldTs, "Switzerland"));
    when(persistanceProvider.getSecurites()).thenReturn(securitiesPersisted);

    testee.update();

    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(2, latestRequest.size());
    assertEquals("[HELN, BALN]", latestRequest.toString());

    verify(persistanceProvider).updateSecurities(securityCollection.capture());
    final Collection<Security> updated = securityCollection.getValue();
    assertEquals(2, updated.size());
    final Security first = updated.stream().filter(security -> "BALN".equals(security.symbol())).findFirst().get();
    assertEquals(oldTs, first.timestamp());
    final Security second = updated.stream().filter(security -> "HELN".equals(security.symbol())).findFirst().get();
    assertEquals(updatedTs, second.timestamp());
  }

  private Collection<Security> getSecurites(final boolean completeForTest, final LocalDateTime timestamp) {
    // complete according to config-example.json
    ArrayList<Security> securites = new ArrayList<>();
    if (completeForTest) {
      securites.add(new Security("BALN", 170.25, "CHF", timestamp, "Switzerland"));
    }
    return securites;
  }


}