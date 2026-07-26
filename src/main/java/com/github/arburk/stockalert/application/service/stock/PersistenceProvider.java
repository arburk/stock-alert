package com.github.arburk.stockalert.application.service.stock;

import com.github.arburk.stockalert.application.domain.MetaInfo;
import com.github.arburk.stockalert.application.domain.Security;
import lombok.NonNull;

import java.util.Collection;
import java.util.Optional;

public interface PersistenceProvider {

  String STORAGE_FILE_NAME = "securities.db.json";

  Collection<Security> getSecurites();

  Optional<Security> getSecurity(@NonNull Security identifier);
  void updateSecurity(@NonNull Security securities);

  MetaInfo getMetaInfo();
  void updateMetaInfo(MetaInfo metaInfo);

  void commitChanges();
}
