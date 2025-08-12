package com.github.arburk.stockalert;

import com.github.arburk.stockalert.application.service.stock.PersistanceProvider;
import com.github.arburk.stockalert.infrastructure.persistance.FileStorage;
import com.github.arburk.stockalert.infrastructure.persistance.s3.S3BucketStorage;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StockAlertApplicationTests {

  @Nested
  @SpringBootTest
  @ActiveProfiles(profiles = "test")
  @TestPropertySource(locations = "classpath:application-test.yml")
  class Defaults {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
      assertInstanceOf(FileStorage.class, context.getBean(PersistanceProvider.class));
    }
  }

  @Nested
  @SpringBootTest
  @ActiveProfiles(profiles = "test")
  @TestPropertySource(locations = "classpath:application-test.yml", properties = {
      "stock-alert.storage-provider=s3",
      "spring.cloud.s3.endpoint.url=https://my.s3.bucket.com",
      "spring.cloud.s3.credentials.access-key=myAccessKey",
  })
  class S3BucketStorageTestClass {

    @Autowired
    private ConfigurableApplicationContext context;

    @Test
    void contextLoads() {
      assertInstanceOf(S3BucketStorage.class, context.getBean(PersistanceProvider.class));
    }
  }
}
