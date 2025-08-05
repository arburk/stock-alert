package com.github.arburk.stockalert.application.service;

import com.github.arburk.stockalert.application.domain.Security;

import java.util.Collection;

public interface Provider {

  Collection<Security> getLatest(Collection<String> symbols);
}
