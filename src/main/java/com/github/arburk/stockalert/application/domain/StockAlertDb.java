package com.github.arburk.stockalert.application.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record StockAlertDb(
    ArrayList<Security> securities,
    MetaInfo metaInfo
) {

  public StockAlertDb {
    if (!securities.isEmpty()) {
      // sort securities by symbol
      securities = new ArrayList<>(getSecuritiesSortedBySymbol(securities));
    }
  }

  private List<Security> getSecuritiesSortedBySymbol(ArrayList<Security> securities) {
    return securities != null && !securities.isEmpty()
        ? securities.stream().filter(Objects::nonNull).sorted(getSymbolComparator()).toList()
        : securities;
  }

  private static Comparator<? super Security> getSymbolComparator() {
    return (Comparator<Security>) (sec1, sec2) -> {
      if (sec1.symbol() == null && sec2.symbol() == null) {
        return 0;
      }
      if (sec1.symbol() == null) {
        return -1;
      }
      if (sec2.symbol() == null) {
        return 1;
      }
      return sec1.symbol().compareTo(sec2.symbol());
    };
  }

}
