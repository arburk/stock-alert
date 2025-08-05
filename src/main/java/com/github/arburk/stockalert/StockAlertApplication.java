package com.github.arburk.stockalert;

import com.github.arburk.stockalert.application.config.StockAlertConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class StockAlertApplication {

	public static void main(String[] args) {

    final ConfigurableApplicationContext app = SpringApplication.run(StockAlertApplication.class, args);
    final StockAlertConfig conf = app.getBean(StockAlertConfig.class);
    log.info("StockAlertConfig: {}", conf);
  }

}
