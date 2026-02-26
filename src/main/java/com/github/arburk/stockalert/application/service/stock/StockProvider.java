package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;

import java.util.Collection;

public interface StockProvider {

  /**
   * Retrieve latest stock data for the given securities.
   * The implementation is responsible for building the correct query format (e.g. EXCHANGE:SYMBOL for FCS API v4).
   */
  Collection<Security> getLatest(Collection<SecurityConfig> securities);
}
