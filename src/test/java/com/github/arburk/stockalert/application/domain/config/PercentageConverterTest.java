package com.github.arburk.stockalert.application.domain.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PercentageConverterTest {

  @Test
  void test_asDouble_emptyOrInvalid() {
    assertNull(PercentageConverter.asDouble(null));
    assertNull(PercentageConverter.asDouble(""));
    assertNull(PercentageConverter.asDouble(" "));
    assertNull(PercentageConverter.asDouble("5 Percentage"));
    assertNull(PercentageConverter.asDouble("Below 5"));
  }

  @Test
  void test_asDouble_decimal() {
    assertEquals(.5, PercentageConverter.asDouble(".5"));
    assertEquals(.5, PercentageConverter.asDouble("0.5"));
    assertEquals(.5, PercentageConverter.asDouble(" 0.500"));
    assertEquals(.5, PercentageConverter.asDouble("0.5%"));
    assertEquals(.5, PercentageConverter.asDouble("0.5 %"));
    assertEquals(.5, PercentageConverter.asDouble("0,5 %"));
  }

  @Test
  void test_asDouble_integer() {
    assertEquals(.05, PercentageConverter.asDouble("5"));
    assertEquals(.05, PercentageConverter.asDouble(" 5 "));
    assertEquals(.05, PercentageConverter.asDouble("5.0"));
    assertEquals(.05, PercentageConverter.asDouble("5,0"));
    assertEquals(.05, PercentageConverter.asDouble(" 5.00 "));
    assertEquals(.05, PercentageConverter.asDouble("5%"));
    assertEquals(.05, PercentageConverter.asDouble("5 %"));
    assertEquals(.05, PercentageConverter.asDouble("5.0 %"));
  }

}