package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.service.notification.Channel;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Disabled("Enable this test to verify SMTP settings with certain provider")
@SpringBootTest
@ActiveProfiles(profiles = "test")
@TestPropertySource(locations = "classpath:application-test.yml", properties = {
    "spring.mail.host=localhost",
    "spring.mail.port=465",
    "spring.mail.username=tesuser@loclahost",
    "spring.mail.password=<password>",
    "spring.mail.properties.mail.smtp.starttls.enable=false",
    "spring.mail.properties.mail.smtp.ssl.enable=true",
    "spring.mail.properties.mail.smtp.sender-address=sender@localhost",
    "stock-alert.config-url=.run/config.json"
})
class EmailNotificationSenderIntegrationTest {

  @Autowired
  private EmailNotificationSender testee;

  @Autowired
  ApplicationConfig applicationConfig;

  @Test
  void name() {
    final AlertConfig testAlertConfig = new AlertConfig(12.25, Channel.EMAIL.getValue(), null);

    LocalDateTime persistedTs = LocalDateTime.of(2025, Month.JULY, 17, 12, 16, 24, 12);
    LocalDateTime updatedTs = LocalDateTime.of(2025, Month.AUGUST, 12, 9, 16, 17, 34);
    final Security persisted = new Security("ABC", 12.0, "CHF", null, persistedTs, "Switzerland", null);
    final Security latest = new Security("ABC", 13.0, "CHF", null, updatedTs, "Switzerland", null);

    assertDoesNotThrow(() -> testee.send(applicationConfig.getStockAlertsConfig(), testAlertConfig, latest, persisted));
  }
}