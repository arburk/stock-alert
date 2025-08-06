package com.github.arburk.stockalert.application.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration()
@ConfigurationProperties(prefix = "stock-alert.gateways")
public class GatewayConfig {

  private String email;

}
