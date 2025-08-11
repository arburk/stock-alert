package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
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
  public Collection<Security> getLatest(final Collection<String> symbols) {
    if (symbols == null || symbols.isEmpty()) {
      log.warn("No symbols provided");
      return Collections.emptyList();
    }

    return readRemoteData(symbols.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .distinct()
        .collect(Collectors.joining(",")));
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
