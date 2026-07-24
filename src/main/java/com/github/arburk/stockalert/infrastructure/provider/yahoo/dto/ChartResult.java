package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChartResult(
    Meta meta
) {}
