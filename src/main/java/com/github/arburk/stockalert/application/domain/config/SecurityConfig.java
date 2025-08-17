package com.github.arburk.stockalert.application.domain.config;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecurityConfig {

  private String symbol;
  private String exchange;
  private List<Alert> alerts;

}
