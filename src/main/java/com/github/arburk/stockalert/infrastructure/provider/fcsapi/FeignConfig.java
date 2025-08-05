package com.github.arburk.stockalert.infrastructure.provider.fcsapi;

import feign.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

  @Bean
  Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL;  // shows everything (Header, Body, etc.)
  }

}
