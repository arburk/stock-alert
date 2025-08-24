package com.github.arburk.stockalert.application.domain.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SecurityConfig(
    String symbol,
    String exchange,
    String isin,
    String _comment,
    List<Alert> alerts
) {
}
