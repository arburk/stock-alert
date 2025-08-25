package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityMapperTest {

  @Test
  void mappingTestHappyFlow() {

    final StockItem input = new StockItem("31.94","32.28","31.68","-0.2","-0.62%","1754337599","DVN","united-states","USD","NYSE","1298", "2025-08-04 19:59:59");

    final Security output = SecurityMapper.INSTANCE.fromStockItem(input);
    assertNotNull(output);
    assertEquals("DVN", output.symbol());
    assertEquals(31.94, output.price());
    assertEquals("USD", output.currency());
    assertEquals(-.0062, output.changePercentage());
    assertEquals(LocalDateTime.of(2025, Month.AUGUST, 4, 19, 59, 59)
        , output.timestamp());
    assertEquals("NYSE", output.exchange());
    assertEquals("Security[symbol=DVN, price=31.94, currency=USD, changePercentage=-0.0062, timestamp=2025-08-04T19:59:59, exchange=NYSE]", output.toString());
  }

  @Test
  void changePercentageStringToDouble() {
    assertEquals(-0.0167, SecurityMapper.INSTANCE.changePercentageStringToDouble("-1.67%"));
    assertEquals(0.0184, SecurityMapper.INSTANCE.changePercentageStringToDouble("1.84%"));
    assertEquals(-0.0111, SecurityMapper.INSTANCE.changePercentageStringToDouble("-1.11%"));
    assertEquals(0.0896, SecurityMapper.INSTANCE.changePercentageStringToDouble("8.96%"));
    assertEquals(0.00, SecurityMapper.INSTANCE.changePercentageStringToDouble("0%"));
    assertEquals(-0.0066, SecurityMapper.INSTANCE.changePercentageStringToDouble("-0.66%"));
    assertEquals(0.25, SecurityMapper.INSTANCE.changePercentageStringToDouble("25%"));
    assertEquals(0.0037, SecurityMapper.INSTANCE.changePercentageStringToDouble("0.37%"));
  }
}