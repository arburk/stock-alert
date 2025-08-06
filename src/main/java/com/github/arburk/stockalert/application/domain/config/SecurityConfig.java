package com.github.arburk.stockalert.application.domain.config;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SecurityConfig {

  private String symbol;
  private String exchange;
  private List<Alert> alerts;

}
