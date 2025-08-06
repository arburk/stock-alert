package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.domain.Security;

import java.util.Collection;

public interface StockProvider {

  Collection<Security> getLatest(Collection<String> symbols);
}
