package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.config.JacksonConfig;
import com.github.arburk.stockalert.application.domain.Alert;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.application.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(getSecurites(true, updatedTs));
    final Collection<Security> testSecurities = getSecurites(true, oldTs);
    when(persistenceProvider.getSecurites()).thenReturn(testSecurities);

    testee.update();

    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(1, latestRequest.size());
    assertEquals("[BALN]", latestRequest.toString());
    testSecurities.forEach(security -> verify(persistenceProvider).updateSecurity(security));
    verify(persistenceProvider).commitChanges();
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
    final LocalDateTime updatedTs = LocalDateTime.now();
    final LocalDateTime oldTs = LocalDateTime.of(2025, Month.JULY, 4, 17, 25, 32);

    final Collection<Security> providedSecurities = getSecurites(false, updatedTs);
    providedSecurities.add(new Security("HELN", 176.25, "CHF", null, updatedTs, "Switzerland", null));
    when(stockProvider.getLatest(stringCollection.capture())).thenReturn(providedSecurities);

    final Collection<Security> securitiesPersisted = getSecurites(true, oldTs);
    securitiesPersisted.add(new Security("HELN", 176.25, "CHF", null, oldTs, "Switzerland", null));
    when(persistenceProvider.getSecurites()).thenReturn(securitiesPersisted);

    testee.update();

    final Collection<String> latestRequest = stringCollection.getValue();
    assertEquals(2, latestRequest.size());
    assertEquals("[HELN, BALN]", latestRequest.toString());

    providedSecurities.forEach(security -> verify(persistenceProvider).updateSecurity(security));
    verify(persistenceProvider).commitChanges();
  }

  private Collection<Security> getSecurites(final boolean completeForTest, final LocalDateTime timestamp) {
    // complete according to config-example.json
    ArrayList<Security> securites = new ArrayList<>();
    if (completeForTest) {
      securites.add(new Security("BALN", 170.25, "CHF", null, timestamp, "Switzerland", null));
    }
    return securites;
  }

  @Nested
  class CheckAndRaisePriceAlert {

    private static final LocalDateTime CURRENT_TIMESTAMP = LocalDateTime.now();
    private static final AlertConfig email = new AlertConfig(101.,"email", "test");
    private static final SecurityConfig SECURITY_CONFIG = new SecurityConfig("ABC", "SIX", null, null, null, List.of(email));

    private Security persisted;

    @BeforeEach
    void setUp() {
      persisted = new Security("ABC", 100., "CHF", null, null, "SIX", null);
      when(persistenceProvider.getSecurity(Security.fromConfig(SECURITY_CONFIG))).thenReturn(Optional.of(persisted));
    }

    @Test
    void checkAndRaisePriceAlert_EmptyLog() {
      final Security latestExceedsThreshold = new Security("ABC", 102., "CHF", null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkSecurityAndRaiseAlert", applicationConfig.getStockAlertsConfig(), SECURITY_CONFIG, Optional.of(latestExceedsThreshold));

      verify(notifyService).send(applicationConfig.getStockAlertsConfig(), email, latestExceedsThreshold, persisted);
      assertFalse(persisted.alertLog().isEmpty());
      assertEquals(1 , persisted.alertLog().size());
      final Alert alertAdd = persisted.alertLog().stream().toList().getFirst();
      assertTrue(alertAdd.timestamp().isAfter(CURRENT_TIMESTAMP));
      assertEquals(101,  alertAdd.threshold());
      assertEquals("CHF", alertAdd.unit());
    }

    @Test
    void checkAndRaisePriceAlert_OutdatedLog() {
      final Alert outdatedEntry = new Alert(CURRENT_TIMESTAMP.minusHours(1), 101., "CHF");
      persisted.alertLog().add(outdatedEntry);
      final Security latestExceedsThreshold = new Security("ABC", 102., "CHF", null, CURRENT_TIMESTAMP, null,null);
      ReflectionTestUtils.invokeMethod(testee, "checkSecurityAndRaiseAlert", applicationConfig.getStockAlertsConfig(), SECURITY_CONFIG, Optional.of(latestExceedsThreshold));

      verify(notifyService).send(applicationConfig.getStockAlertsConfig(), email, latestExceedsThreshold, persisted);
      assertFalse(persisted.alertLog().isEmpty());
      assertEquals(1 , persisted.alertLog().size());
      final Alert alertAdd = persisted.alertLog().stream().toList().getFirst();
      assertTrue(alertAdd.timestamp().isAfter(CURRENT_TIMESTAMP));
      assertEquals(101,  alertAdd.threshold());
      assertEquals("CHF", alertAdd.unit());
    }

    @Test
    void checkAndRaisePriceAlert_CurrenLog_OutdatedStockInfo_SkipNotification() {
      final Alert currentEntry = new Alert(CURRENT_TIMESTAMP, 101., "CHF");
      persisted.alertLog().add(currentEntry);
      final Security latestExceedsThreshold = new Security("ABC", 102., "CHF", null, CURRENT_TIMESTAMP.minusHours(1), null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkSecurityAndRaiseAlert", applicationConfig.getStockAlertsConfig(), SECURITY_CONFIG, Optional.of(latestExceedsThreshold));

      verify(notifyService, never()).send(applicationConfig.getStockAlertsConfig(), email, latestExceedsThreshold, persisted);
      assertFalse(persisted.alertLog().isEmpty());
      assertEquals(1 , persisted.alertLog().size());
      final Alert alertAdd = persisted.alertLog().stream().toList().getFirst();
      assertEquals(currentEntry, alertAdd);
    }
  }

  @Nested
  class CheckAndRaisePercentageAlert {

    private static final Security PERSISTED = new Security(null, 100., null, null, null, null, null);
    private static final SecurityConfig EMPTY_CONFIG = new SecurityConfig(null, null, null, null, null, null);

    @Test
    void checkAndRaisePercentageAlert_Increased() {
      final Security latestExceedsThreshold = new Security(null, 105., null, null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latestExceedsThreshold, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latestExceedsThreshold), eq(PERSISTED), eq(0.05), captor.capture());
      assertTrue(captor.getValue() >= 0.05);
    }

    @Test
    void checkAndRaisePercentageAlert_Decreased() {
      final SecurityConfig config = new SecurityConfig(null, null, null, null, "0.05", null);
      final Security latestDecreasedCrossingThreshold = new Security(null, 95., null, null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), config, latestDecreasedCrossingThreshold, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latestDecreasedCrossingThreshold), eq(PERSISTED), eq(0.05), captor.capture());
      assertTrue(captor.getValue() <= 0.05);
    }

    @Test
    void checkAndRaisePercentageAlert_NotRequired() {
      final Security latestWithinBoundary = new Security(null, 96., null, null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latestWithinBoundary, PERSISTED);
      verify(notifyService, never()).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), any(Security.class), any(Security.class), eq(0.05), anyDouble());
    }

    @Test
    void checkAndRaisePercentageAlert_ProvideValueTrigger() {
      final Security latest = new Security(null, 96., null, .0536, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), EMPTY_CONFIG, latest, PERSISTED);

      ArgumentCaptor<Double> captor = ArgumentCaptor.forClass(Double.class);
      verify(notifyService).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), eq(latest), eq(PERSISTED), eq(0.05), captor.capture());
      assertEquals(.0536, captor.getValue());
    }

    @Test
    void checkAndRaisePercentageAlert_GlobalValueResetted() {
      final SecurityConfig config = new SecurityConfig(null, null, null, null, "0", null);
      final Security latest = new Security(null, 90., null, null, null, null, null);
      ReflectionTestUtils.invokeMethod(testee, "checkAndRaisePercentageAlert", applicationConfig.getStockAlertsConfig(), config, latest, PERSISTED);
      verify(notifyService, never()).sendPercentage(eq(applicationConfig.getStockAlertsConfig()), any(Security.class), any(Security.class), eq(0.05), anyDouble());
    }
  }

  @Nested
  class SkipProvidedDueToSilencer {

    @Test
    void skipProvidedDueToSilencer_notProvided_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      assertResultIsFalse(applicationConfig.getStockAlertsConfig(), false, latestSecurity);
    }

    @Test
    void skipProvidedDueToSilencer_silenceDurationNotProvided_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(null);
      assertResultIsFalse(stockAlertsConfig, true, latestSecurity);
    }

    @Test
    void skipProvidedDueToSilencer_silenceDurationZero_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(Duration.ZERO);
      assertResultIsFalse(stockAlertsConfig, true, latestSecurity);
    }

    @Test
    void skipProvidedDueToSilencer_unpersistedSecurity_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(Duration.ofMinutes(1));
      when(persistenceProvider.getSecurity(any(Security.class))).thenReturn(Optional.empty());
      assertResultIsFalse(stockAlertsConfig, true, latestSecurity);
    }

    @Test
    void skipProvidedDueToSilencer_persistedSecurityWithoutAlert_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(Duration.ofMinutes(1));
      when(persistenceProvider.getSecurity(any(Security.class))).thenReturn(Optional.of(latestSecurity));
      assertResultIsFalse(stockAlertsConfig, true, latestSecurity);
    }

    @Test
    void skipProvidedDueToSilencer_persistedSecurity_RecentAlertTooOld_isFalse() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(Duration.ofMinutes(2));
      final Collection<Alert> alertLog = Arrays.asList(
          new Alert(LocalDateTime.now().minusMinutes(5), null, null),
          new Alert(LocalDateTime.now().minusDays(1), null, null)
      );
      final Security persistedSecurity = new Security(null, null, null, null, null, null, alertLog);

      when(persistenceProvider.getSecurity(any(Security.class))).thenReturn(Optional.of(persistedSecurity));
      assertResultIsFalse(stockAlertsConfig, true, latestSecurity);
    }

    private void assertResultIsFalse(final StockAlertsConfig stockAlertsConfig, final boolean isProvided, final Security latestSecurity) {
      final Boolean result = ReflectionTestUtils.invokeMethod(testee, "skipProvidedDueToSilencer", stockAlertsConfig, isProvided, latestSecurity);
      assertNotNull(result);
      assertFalse(result);
    }

    @Test
    void skipProvidedDueToSilencer_persistedSecurity_RecentAlertToYoung_isTrue() {
      final Security latestSecurity = new Security(null, null, null, null, null, null, null);
      var stockAlertsConfig = mock(StockAlertsConfig.class);
      when(stockAlertsConfig.getSilenceDuration()).thenReturn(Duration.ofMinutes(2));
      final Collection<Alert> alertLog = Arrays.asList(
          new Alert(LocalDateTime.now(), null, null),
          new Alert(LocalDateTime.now().minusDays(1), null, null)
      );
      final Security persistedSecurity = new Security(null, null, null, null, null, null, alertLog);

      when(persistenceProvider.getSecurity(any(Security.class))).thenReturn(Optional.of(persistedSecurity));
      final Boolean result = ReflectionTestUtils.invokeMethod(testee, "skipProvidedDueToSilencer", stockAlertsConfig, true, latestSecurity);
      assertNotNull(result);
      assertTrue(result);
    }

  }
}