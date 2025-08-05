package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.github.arburk.stockalert.application.domain.Security;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

class SecurityMapperTest {

  @Test
  void mappingTestHappyFlow() {

    final StockItem input = new StockItem("31.94","32.28","31.68","-0.2","-0.62%","1754337599","DVN","united-states","USD","NYSE","1298", "2025-08-04 19:59:59");

    final Security output = SecurityMapper.INSTANCE.fromStockItem(input);
    assertNotNull(output);
    assertEquals("DVN", output.symbol());
    assertEquals(31.94, output.price());
    assertEquals("USD", output.currency());
    assertEquals(LocalDateTime.of(2025, Month.AUGUST, 4, 19, 59, 59)
        , output.timestamp());
    assertEquals("NYSE", output.exchange());
    assertEquals("Security[symbol=DVN, price=31.94, currency=USD, timestamp=2025-08-04T19:59:59, exchange=NYSE]", output.toString());
  }
}