package com.github.arburk.stockalert.application.domain;

import java.time.LocalDateTime;

public record Security(
    String symbol,
    Double price,
    String currency,
    LocalDateTime timestamp,
    String exchange
) {
}
