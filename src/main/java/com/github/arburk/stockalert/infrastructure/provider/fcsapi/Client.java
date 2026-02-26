package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.service.stock.StockProvider;
import com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto.SecurityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class Client implements StockProvider {

  private final StockClient stockClient;
  private final ApplicationConfig applicationConfig;

  public Client(final StockClient stockClient, ApplicationConfig applicationConfig) {
    this.stockClient = stockClient;
    this.applicationConfig = applicationConfig;
    log.info("StockClient instance created: {}", stockClient);
  }

  @Override
  public Collection<Security> getLatest(final Collection<SecurityConfig> securities) {
    if (securities == null || securities.isEmpty()) {
      log.warn("No securities provided");
      return Collections.emptyList();
    }

    // v4 API requires EXCHANGE:SYMBOL format
    final String symbolsCsv = securities.stream()
        .filter(Objects::nonNull)
        .filter(s -> s.symbol() != null && s.exchange() != null)
        .map(s -> s.exchange().trim() + ":" + s.symbol().trim())
        .distinct()
        .collect(Collectors.joining(","));

    if (symbolsCsv.isBlank()) {
      log.warn("No valid symbols/exchanges found in provided securities");
      return Collections.emptyList();
    }

    return readRemoteData(symbolsCsv);
  }

  private Collection<Security> readRemoteData(final String symbolsCsv) {
    final var response = stockClient.getLatestStocks(symbolsCsv, applicationConfig.getFcsApiKey());
    log.info(response.info().toString());

    //TODO: register response.info().getCreditCount() to monitor limits

    if (response.status()) {
      return response.response().stream()
          .peek(stock -> log.debug("{}: {} {} ({})", stock.s(), stock.c(), stock.ccy(), stock.cp()))
          .map(SecurityMapper.INSTANCE::fromStockItem)
          .collect(Collectors.toSet());
    }

    log.error("Status {}: {}", response.code(), response.msg());
    return Collections.emptyList();
  }

}
