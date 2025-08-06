package com.github.arburk.stockalert.infrastructure.persistance;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.service.PersistanceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class FileStorage implements PersistanceProvider {

  @Override
  public Collection<Security> getSecurites() {
    //TODO: implement me
    log.error("implement FileStorage#getSecurites");
    return List.of();
  }

  @Override
  public void updateSecurities(final Collection<Security> securities) {
    //TODO: implement me
    log.error("implement FileStorage#updateSecurities");
  }

}
