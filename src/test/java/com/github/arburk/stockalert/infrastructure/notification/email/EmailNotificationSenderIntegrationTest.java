package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
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

  @Test
  void name() {
    final Alert testAlert = new Alert();
    testAlert.setNotification(Channel.EMAIL.getValue());
    testAlert.setThreshold(12.25);

    LocalDateTime persistedTs = LocalDateTime.of(2025, Month.JULY, 17, 12, 16, 24, 12);
    LocalDateTime updatedTs = LocalDateTime.of(2025, Month.AUGUST, 12, 9, 16, 17, 34);
    final Security persisted = new Security("ABC", 12.0, "CHF", persistedTs, "Switzerland");
    final Security latest = new Security("ABC", 13.0, "CHF", updatedTs, "Switzerland");

    assertDoesNotThrow(() -> testee.send(testAlert, latest, persisted));
  }
}