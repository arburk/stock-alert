package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Chart(
    List<ChartResult> result,
    ChartError error
) {}
