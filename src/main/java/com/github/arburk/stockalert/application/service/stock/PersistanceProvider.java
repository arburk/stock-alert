package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.Security;

import java.util.Collection;

public interface PersistanceProvider {

  String STORAGE_FILE_NAME = "securities.db.json";

  @Deprecated(since = "0.1.4", forRemoval = true)
  String STORAGE_FILE_NAME_0_1_3 = "securities.db";

  Collection<Security> getSecurites();

  void updateSecurities(Collection<Security> securities);

}
