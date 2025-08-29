package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

  private ApplicationConfig applicationConfig;
  private StockProvider stockProvider;
  private PersistenceProvider persistenceProvider;
  private NotificationService notifyService;

  private StockService testee;

  @BeforeEach
  void setUp() {
    applicationConfig = new ApplicationConfig(new JacksonConfig().objectMapper());
    applicationConfig.setConfigUrl(Path.of("src/main/resources/config-example.json").toUri().toString());
    stockProvider = Mockito.mock(StockProvider.class);
    notifyService = Mockito.mock(NotificationService.class);
    persistenceProvider = Mockito.mock(PersistenceProvider.class);
    testee = new StockService(applicationConfig, stockProvider, persistenceProvider, notifyService);
  }

  @Test
  void updateHappyFlow() {
    final ArgumentCaptor<Collection<String>> stringCollection = ArgumentCaptor.forClass(Collection.class);
    final ArgumentCaptor<Collection<Security>> securityCollection = ArgumentCaptor.forClass(Collection.class);
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(getSecurites(true, updatedTs));
    when(persistenceProvider.getSecurites()).thenReturn(getSecurites(true, oldTs));

    testee.update();

    verify(persistenceProvider).updateSecurities(securityCollection.capture());
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
    when(persistenceProvider.getSecurites()).thenReturn(getSecurites(true, oldTs));

    testee.update();

    verify(persistenceProvider, never()).updateSecurities(any());
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
    providedSecurities.add(new Security("HELN", 176.25, "CHF", null, updatedTs, "Switzerland"));
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(providedSecurities);

    final Collection<Security> securitiesPersisted = getSecurites(true, oldTs);
    securitiesPersisted.add(new Security("HELN", 176.25, "CHF", null, oldTs, "Switzerland"));
    when(persistenceProvider.getSecurites()).thenReturn(securitiesPersisted);

    testee.update();

    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(2, latestRequest.size());
    assertEquals("[HELN, BALN]", latestRequest.toString());

    verify(persistenceProvider).updateSecurities(securityCollection.capture());
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
      securites.add(new Security("BALN", 170.25, "CHF", null, timestamp, "Switzerland"));
    }
    return securites;
  }

  @Nested
  class CheckAndRaisePercentageAlert {

    public static final Security PERSISTED = new Security(null, 100., null, null, null, null);
    private static final SecurityConfig EMPTY_CONFIG = new SecurityConfig(null, null, null, null, null, null);

    @Test
    void checkAndRaisePercentageAlert_Increased() {
      final Security latestExceedsThreshold = new Security(null, 105., null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latestExceedsThreshold, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latestExceedsThreshold), eq(PERSISTED), eq(0.05), captor.capture());
      assertTrue(captor.getValue() >= 0.05);
    }

    @Test
    void checkAndRaisePercentageAlert_Decreased() {
      final SecurityConfig config = new SecurityConfig(null, null, null, null, "0.05", null);
      final Security latestDecreasedCrossingThreshold = new Security(null, 95., null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), config, latestDecreasedCrossingThreshold, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latestDecreasedCrossingThreshold), eq(PERSISTED), eq(0.05), captor.capture());
      assertTrue(captor.getValue() <= 0.05);
    }

    @Test
    void checkAndRaisePercentageAlert_NotRequired() {
      final Security latestWithinBoundary = new Security(null, 96., null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latestWithinBoundary, PERSISTED);
      verify(notifyService, never()).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), any(Security.class), any(Security.class), eq(0.05), anyDouble());
    }

    @Test
    void checkAndRaisePercentageAlert_ProvideValueTrigger() {
      final Security latest = new Security(null, 96., null, .0536, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latest, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latest), eq(PERSISTED), eq(0.05), captor.capture());
      assertEquals(.0536, captor.getValue());
    }

    @Test
    void checkAndRaisePercentageAlert_GlobalValueResetted() {
      final SecurityConfig config = new SecurityConfig(null, null, null, null, "0", null);
      final Security latest = new Security(null, 90., null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), config, latest, PERSISTED);
      verify(notifyService, never()).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), any(Security.class), any(Security.class), eq(0.05), anyDouble());
    }
  }
}