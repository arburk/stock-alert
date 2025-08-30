package com.github.arburk.stockalert.infrastructure.persistance;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.StockAlertDb;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractPersistenceProviderTest extends AbstractPersistenceProvider {


  private static final Security TEST_SECURITY_A = new Security("TST_A", 12.0, "CHF", -2.27, LocalDateTime.now(), "Switzerland", null);
  private static final Security TEST_SECURITY_B = new Security("TST_B", 124.57, "EUR", 0.69, LocalDateTime.now(), "Tradegate", null);

  @Test
  void getSecurity_Found() {
    final Optional<Security> security = getSecurity(new Security(TEST_SECURITY_B.symbol(), null, null, null, null, TEST_SECURITY_B.exchange(), null));
    assertTrue(security.isPresent());
    assertEquals(TEST_SECURITY_B, security.get());
  }

  @Test
  void getSecurity_NotFound() {
    final Optional<Security> security = getSecurity(new Security(TEST_SECURITY_B.symbol(), null, null, null, null, TEST_SECURITY_A.exchange(), null));
    assertNotNull(security);
    assertFalse(security.isPresent());
  }

  @Test
  void updateExistingSecurity() {
    Collection<Security> securites = this.getSecurites();
    assertNotNull(securites);
    assertEquals(2, securites.size());
    assertTrue(securites.contains(TEST_SECURITY_B));
    assertTrue(securites.contains(TEST_SECURITY_A));

    final Security updatedSecurity = new Security(TEST_SECURITY_B.symbol(), TEST_SECURITY_B.price() + 1, TEST_SECURITY_B.currency(), null, LocalDateTime.now().plusSeconds(1), TEST_SECURITY_B.exchange(), null);
    this.updateSecurity(updatedSecurity);

    securites = this.getSecurites();
    assertNotNull(securites);
    assertEquals(2, securites.size());
    assertFalse(securites.stream().map(Security::hashCode).toList().contains(TEST_SECURITY_B.hashCode()));
    assertTrue(securites.stream().map(Security::hashCode).toList().contains(updatedSecurity.hashCode()));
    assertTrue(securites.stream().map(Security::hashCode).toList().contains(TEST_SECURITY_A.hashCode()));
  }

  @Test
  void updateNewSecurity() {
    Collection<Security> securites = this.getSecurites();
    assertNotNull(securites);
    assertEquals(2, securites.size());
    assertTrue(securites.contains(TEST_SECURITY_B));
    assertTrue(securites.contains(TEST_SECURITY_A));

    final Security updatedSecurity = new Security("NEW", 44.44, "USD", null, LocalDateTime.now(), "NSE", null);
    this.updateSecurity(updatedSecurity);

    securites = this.getSecurites();
    assertNotNull(securites);
    assertEquals(3, securites.size());
    assertTrue(securites.stream().map(Security::hashCode).toList().contains(TEST_SECURITY_A.hashCode()));
    assertTrue(securites.stream().map(Security::hashCode).toList().contains(TEST_SECURITY_A.hashCode()));
    assertTrue(securites.stream().map(Security::hashCode).toList().contains(updatedSecurity.hashCode()));
  }

  @Override
  StockAlertDb initData() {
    final ArrayList<Security> testSecurities = new ArrayList<>(Arrays.asList(TEST_SECURITY_A, TEST_SECURITY_B));
    return new StockAlertDb(testSecurities, null);
  }

  @Override
  public void commitChanges() {
    // unused here as not relevant for this test
  }
}