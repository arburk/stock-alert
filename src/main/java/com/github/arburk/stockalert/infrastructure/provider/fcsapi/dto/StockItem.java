package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockItem(
    String c,     // current price
    String h,     // high
    String l,     // low
    String ch,    // change
    String cp,    // change percent
    String t,     // timestamp
    String s,     // symbol
    String cty,   // country
    String ccy,   // currency
    String exch,  // exchange
    String id,
    String tm    // time
) {}
