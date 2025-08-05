package com.github.arburk.stockalert.application.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StockAlertConfigTest {

  private final StockAlertConfig testee = new StockAlertConfig();

  @Test
  void keyIsMasked_Null() {
    testee.setFcsApiKey(null);
    assertNull(testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=null}", testee.toString());
  }

  @Test
  void keyIsMasked_Empty() {
    testee.setFcsApiKey("");
    assertEquals("", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=}", testee.toString());

    testee.setFcsApiKey("  ");
    assertEquals("", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=}", testee.toString());
  }

  @Test
  void keyIsFullyMasked() {
    testee.setFcsApiKey("A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=*}", testee.toString());

    testee.setFcsApiKey(" A");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=*}", testee.toString());

    testee.setFcsApiKey(" A ");
    assertEquals("A", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=*}", testee.toString());

    testee.setFcsApiKey("AB");
    assertEquals("AB", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=**}", testee.toString());

    testee.setFcsApiKey("ABc");
    assertEquals("ABc", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=***}", testee.toString());

    testee.setFcsApiKey("ABcD");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=****}", testee.toString());

    testee.setFcsApiKey(" ABcD ");
    assertEquals("ABcD", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=****}", testee.toString());
  }

  @Test
  void keyIsPartiallyMasked() {
    testee.setFcsApiKey("MyApiKey");
    assertEquals("MyApiKey", testee.getFcsApiKey());
    assertEquals("StockAlertConfig{fcsApiKey=My****ey}", testee.toString());
  }
}