package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityMapperTest {

  @Test
  void mappingTestHappyFlow() {
    final Meta input = new Meta("USD", "DVN", "NYQ", "NYSE", 31.94, 32.14, 32.14, 1754337599L);

    final Security output = SecurityMapper.INSTANCE.fromChartMeta(input, "NYSE");
    assertNotNull(output);
    assertEquals("DVN", output.symbol());
    assertEquals(31.94, output.price());
    assertEquals("USD", output.currency());
    assertEquals(-.0062, output.changePercentage());
    assertNotNull(output.timestamp());
    assertEquals(1754337599L, output.timestamp().atZone(ZoneId.systemDefault()).toEpochSecond(),
        "timestamp is expected to represent the epoch seconds in the system default zone");
    assertEquals("NYSE", output.exchange());
    assertNotNull(output.alertLog());
  }

  @Test
  void exchangeIsStampedFromParameterNotFromMeta() {
    final Meta input = new Meta("CHF", "BALN.SW", "EBS", "Swiss Exchange", 207.4, 205.0, 205.0, 1754555700L);

    final Security output = SecurityMapper.INSTANCE.fromChartMeta(input, "Switzerland");

    assertEquals("Switzerland", output.exchange());
  }

  @Test
  void calcChangePercentage_RoundingTable() {
    assertEquals(-0.0167, calc(98.33, 100.0));
    assertEquals(0.0184, calc(101.84, 100.0));
    assertEquals(-0.0111, calc(88.90, 89.898));
    assertEquals(0.0896, calc(108.96, 100.0));
    assertEquals(0.00, calc(100.0, 100.0));
    assertEquals(-0.0062, calc(31.94, 32.14));
    assertEquals(0.25, calc(125.0, 100.0));
    assertEquals(0.0037, calc(100.37, 100.0));
    assertEquals(0.0649, calc(20.18, 18.95));
  }

  @Test
  void calcChangePercentage_FallbackToPreviousClose() {
    final Meta input = new Meta("CHF", "VETN.SW", "EBS", "Swiss Exchange", 31.94, null, 32.14, null);
    assertEquals(-0.0062, SecurityMapper.INSTANCE.calcChangePercentage(input));
  }

  @Test
  void calcChangePercentage_NullCases() {
    assertNull(SecurityMapper.INSTANCE.calcChangePercentage(null));
    assertNull(SecurityMapper.INSTANCE.calcChangePercentage(new Meta(null, null, null, null, null, 32.14, 32.14, null)), "null price");
    assertNull(SecurityMapper.INSTANCE.calcChangePercentage(new Meta(null, null, null, null, 31.94, null, null, null)), "no previous close at all");
    assertNull(SecurityMapper.INSTANCE.calcChangePercentage(new Meta(null, null, null, null, 31.94, 0.0, 0.0, null)), "previous close of 0 must not divide");
  }

  @Test
  void epochToLocalDateTime_NullSafe() {
    assertNull(SecurityMapper.INSTANCE.epochToLocalDateTime(null));
  }

  private static Double calc(final double price, final double previousClose) {
    return SecurityMapper.INSTANCE.calcChangePercentage(new Meta(null, null, null, null, price, previousClose, previousClose, null));
  }
}
