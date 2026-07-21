package com.github.arburk.stockalert.infrastructure.provider.yahoo;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

  /**
   * Yahoo rejects requests without a browser-like User-Agent with HTTP 429.
   */
  static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

  @Bean
  Logger.Level feignLoggerLevel() {
    return Logger.Level.FULL;  // shows everything (Header, Body, etc.)
  }

  @Bean
  RequestInterceptor yahooHeaderInterceptor() {
    return requestTemplate -> requestTemplate
        .header("User-Agent", USER_AGENT)
        .header("Accept", "application/json");
  }

}
