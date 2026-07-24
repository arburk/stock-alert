package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import java.util.Collection;
import java.util.List;

public interface StockProvider {

  Collection<Security> getLatest(List<SecurityConfig> securities);
}
