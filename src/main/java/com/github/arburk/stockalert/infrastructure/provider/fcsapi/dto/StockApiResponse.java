package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockApiResponse(
  boolean status,
  int code,
  String msg,
  List<StockItem> response,
  Info info
){}
