package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChartError(
    String code,
    String description
) {}
