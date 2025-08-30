package com.github.arburk.stockalert.infrastructure.persistance;

import com.github.arburk.stockalert.application.domain.MetaInfo;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.StockAlertDb;
import com.github.arburk.stockalert.application.service.stock.PersistenceProvider;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Slf4j
public abstract class AbstractPersistenceProvider implements PersistenceProvider {

  @Getter()
  private StockAlertDb data;

  @Override
  public Collection<Security> getSecurites() {
    if (data == null || data.securities() == null || data.securities().isEmpty()) {
      data = initData();
    }
    return data.securities();
  }

  @Override
  public MetaInfo getMetaInfo() {
    if (data == null) {
      data = initData();
    }
    return data.metaInfo();
  }

  @Override
  public void updateSecurities(final Collection<Security> securities) {
    if (securities == null || securities.isEmpty()) {
      log.warn("updateSecurities was called with empty securities. skip to prevent data loss.");
      log.info("If you want to reset data, stop the application and delete storage file '{}'.", STORAGE_FILE_NAME);
      return;
    }

    securities.forEach(latest -> {
      getSecurites().stream().filter(security ->
              security.symbol().equals(latest.symbol()) && security.exchange().equals(latest.exchange()))
          .findFirst().ifPresent(getData().securities()::remove);
      getData().securities().add(latest);
    });

    persist();
  }

  @Override
  public void updateMetaInfo(final MetaInfo metaInfo) {
    if (metaInfo == null) {
      log.warn("updateMetaInfo was called with empty object. skip to prevent data loss.");
      log.info("If you want to reset MetaInfo, provide object with empty values.");
    }
    data = new StockAlertDb(
        data != null
            ? data.securities()
            : new ArrayList<>(/* must not be immutable */),
        metaInfo
    );
    persist();
  }

  @Override
  public Optional<Security> getSecurity(@NonNull Security identifier) {
    final Collection<Security> securites = getSecurites();
    return securites.isEmpty()
        ? Optional.empty()
        : securites.stream().filter(current -> current.equals(identifier)).findFirst();
  }

  @Override
  public void updateSecurity(@NonNull final Security security) {
    final Optional<Security> stored = getSecurity(security);
    stored.ifPresent(value -> data.securities().remove(value));
    data.securities().add(security);
  }

  @Override
  public void commitChanges() {
    persist();
  }

  abstract StockAlertDb initData();

  abstract void persist();
}
