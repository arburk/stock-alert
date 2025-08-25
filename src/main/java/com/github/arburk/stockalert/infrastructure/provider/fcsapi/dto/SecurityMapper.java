package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.github.arburk.stockalert.application.domain.Security;
import io.micrometer.common.util.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Mapper
public interface SecurityMapper {

  DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  SecurityMapper INSTANCE = Mappers.getMapper(SecurityMapper.class);

  @Mapping(source = "s", target = "symbol")
  @Mapping(source = "c", target = "price", qualifiedByName = "stringToDouble")
  @Mapping(source = "cp", target = "changePercentage", qualifiedByName = "changePercentageStringToDouble")
  @Mapping(source = "ccy", target = "currency")
  @Mapping(source = "tm", target = "timestamp", qualifiedByName = "stringToDateTime")
  @Mapping(source = "exch", target = "exchange")
  Security fromStockItem(StockItem item);

  @Named("stringToDouble")
  default Double mapStringToDouble(String value) {
    try {
      return value != null ? Double.parseDouble(value) : null;
    } catch (NumberFormatException e) {
      LoggerFactory.getLogger(SecurityMapper.class).error(e.getMessage());
      return null;
    }
  }

  @Named("stringToDateTime")
  default LocalDateTime mapStringToDateTime(String value) {
    try {
      return value == null || value.isBlank()
          ? null
          : LocalDateTime.parse(value, DATE_TIME_PATTERN);
    } catch (Exception e) {
      LoggerFactory.getLogger(SecurityMapper.class).error(e.getMessage());
      return null;
    }
  }

  /**
   * Domain object expect percentage value between -1 and 1 related to 100%
   * Thus, the input values are converted to double values and divided by 100 with exact 4 decimal place.
   * The reason is that FCSAPI-Api provides cp value like "25.1%"
   */
  @Named("changePercentageStringToDouble")
  default Double changePercentageStringToDouble(String value) {
    return StringUtils.isBlank(value)
        ? null
        : BigDecimal.valueOf(mapStringToDouble(value.replaceAll("[\\s%]+", "")) / 100)
        .setScale(4, RoundingMode.HALF_UP)
        .doubleValue();
  }
}

