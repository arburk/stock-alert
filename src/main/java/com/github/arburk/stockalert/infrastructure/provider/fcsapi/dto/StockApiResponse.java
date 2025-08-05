package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import java.util.List;

public record StockApiResponse(
  boolean status,
  int code,
  String msg,
  List<StockItem> response,
  Info info
){}
