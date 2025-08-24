package com.github.arburk.stockalert.application.domain.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityConfigTest {

  @Test
  void stringRepresentation() {
    assertEquals("SecurityConfig[symbol=null, exchange=null, isin=null, _comment=null, alerts=null]",
        new SecurityConfig(null, null, null, null, null).toString());

    assertEquals("SecurityConfig[symbol=ABC, exchange=SIX, isin=CH0012410517, _comment=yeah, alerts=[]]",
        new SecurityConfig("ABC", "SIX", "CH0012410517", "yeah", List.of()).toString());
  }
}