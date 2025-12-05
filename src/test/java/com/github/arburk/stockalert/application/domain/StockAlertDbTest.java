package com.github.arburk.stockalert.application.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StockAlertDbTest {

  @Test
  void verifySortedSecuritesBySymbol() {
    Security s1 = new Security("abc", null,null,null,null,null,null);
    Security s2 = new Security("ABC", null,null,null,null,null,null);
    Security s3 = new Security("XYZ", null,null,null,null,null,null);
    Security s4 = new Security("DEF", null,null,null,null,null,null);

    final StockAlertDb stockAlertDb = new StockAlertDb(new ArrayList<>(Arrays.asList(s1, s2, s3, s4)), null);
    assertEquals(s2, stockAlertDb.securities().getFirst());
    assertEquals(s4, stockAlertDb.securities().get(1));
    assertEquals(s3, stockAlertDb.securities().get(2));
    assertEquals(s1, stockAlertDb.securities().get(3));
  }

  @Test
  void nullSafe() {
    Security s1 = new Security("abc", null, null, null, null, null, null);
    final StockAlertDb stockAlertDb = new StockAlertDb(new ArrayList<>(Arrays.asList(s1, null)), null);

    assertEquals(1, stockAlertDb.securities().size());
    assertEquals(s1, stockAlertDb.securities().getFirst());
  }

  @Test
  void nullSafeForSymbols() {
    Security s1 = new Security(null, null, null, null, null, null, null);
    Security s2 = new Security("abc", null, null, null, null, null, null);
    assertEquals(s1, new StockAlertDb(new ArrayList<>(Arrays.asList(s1, s2)), null).securities().getFirst());
    assertEquals(s1, new StockAlertDb(new ArrayList<>(Arrays.asList(s2, s1)), null).securities().getFirst());
  }


}