package com.github.arburk.stockalert.infrastructure.provider.yahoo.dto;

import com.github.arburk.stockalert.application.domain.Security;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Mapper
public interface SecurityMapper {

  SecurityMapper INSTANCE = Mappers.getMapper(SecurityMapper.class);

  @Mapping(source = "meta.symbol", target = "symbol")
  @Mapping(source = "meta.regularMarketPrice", target = "price")
  @Mapping(source = "meta", target = "changePercentage", qualifiedByName = "calcChangePercentage")
  @Mapping(source = "meta.currency", target = "currency")
  @Mapping(source = "meta.regularMarketTime", target = "timestamp", qualifiedByName = "epochToLocalDateTime")
  @Mapping(source = "exchange", target = "exchange")
  @Mapping(target = "alertLog", ignore = true)
  Security fromChartMeta(Meta meta, String exchange);

  /**
   * Domain object expects percentage value between -1 and 1 related to 100%.
   * Yahoo meta does not provide a reliable change percentage, so it is derived from
   * regularMarketPrice / chartPreviousClose - 1 (fallback previousClose), rounded to
   * exact 4 decimal places, e.g. -0.62 % -> -0.0062.
   */
  @Named("calcChangePercentage")
  default Double calcChangePercentage(Meta meta) {
    if (meta == null || meta.regularMarketPrice() == null) {
      return null;
    }
    final Double previousClose = meta.chartPreviousClose() != null
        ? meta.chartPreviousClose()
        : meta.previousClose();
    if (previousClose == null || previousClose == 0) {
      return null;
    }
    return BigDecimal.valueOf(meta.regularMarketPrice() / previousClose - 1)
        .setScale(4, RoundingMode.HALF_UP)
        .doubleValue();
  }

  @Named("epochToLocalDateTime")
  default LocalDateTime epochToLocalDateTime(Long epochSeconds) {
    return epochSeconds == null
        ? null
        : LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
  }
}
