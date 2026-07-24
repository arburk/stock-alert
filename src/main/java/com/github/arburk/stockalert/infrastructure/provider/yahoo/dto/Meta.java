package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Meta(
    String currency,
    String symbol,
    String exchangeName,
    String fullExchangeName,
    Double regularMarketPrice,
    Double chartPreviousClose,
    Double previousClose,
    Long regularMarketTime
) {}
