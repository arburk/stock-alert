package com.github.arburk.stockalert.infrastructure.provider.fcsapi.dto;

import com.github.arburk.stockalert.application.domain.Security;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Mapper
public interface SecurityMapper {

  DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  SecurityMapper INSTANCE = Mappers.getMapper(SecurityMapper.class);

  @Mapping(source = "s", target = "symbol")
  @Mapping(source = "c", target = "price", qualifiedByName = "stringToDouble")
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
}

