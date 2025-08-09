package com.github.arburk.stockalert;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.service.Scheduler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "com.github.arburk.stockalert.infrastructure.provider")
public class StockAlertApplication {

	public static void main(String[] args) {
    final ConfigurableApplicationContext app = SpringApplication.run(StockAlertApplication.class, args);
    init(app);
  }

  private static void init(final ConfigurableApplicationContext app) {
    final ApplicationConfig config = app.getBean(ApplicationConfig.class);
    log.info("StockAlertConfig: {}", config);
    if (config.isRunOnStartup()) {
      app.getBean(Scheduler.class).updateStock();
    }
  }

}
