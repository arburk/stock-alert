package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Security;

import java.util.Collection;

public interface PersistanceProvider {

  Collection<Security> getSecurites();

  void updateSecurities(Collection<Security> securities);

}
