package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.domain.config.NotificationChannel;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.application.service.notification.Channel;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotificationSenderTest {

  private JavaMailSender mailSender;
  private ApplicationConfig appConfig;
  private StockAlertsConfig stockAlertsConfig;
  private EmailNotificationSender testee;

  private static final String recipient1 = "me@here.com";
  private static final String recipient2 = "myself@there.com";
  private static final String recipient3 = "i@outa-space.com";


  @BeforeEach
  void setUp() {
    stockAlertsConfig = new StockAlertsConfig();
    mailSender = Mockito.mock(JavaMailSender.class);
    appConfig = Mockito.mock(ApplicationConfig.class);
    when(appConfig.getStockAlertsConfig()).thenReturn(stockAlertsConfig);
    testee = new EmailNotificationSender(mailSender, appConfig);
  }

  @Test
  void verifyChannelSetting() {
    assertEquals(Channel.EMAIL, testee.getChannel());
  }

  @Test
  void testRecipientsExtraction() {
    final NotificationChannel mailChannel = new NotificationChannel();
    mailChannel.setRecipients(recipient1 + ", " + recipient2 + " ; " + recipient3);
    mailChannel.setType(Channel.EMAIL.getValue());
    stockAlertsConfig.setNotificationChannels(List.of(mailChannel));

    final String[] recipients = testee.getRecipients(appConfig);

    assertEquals(3, recipients.length);
    assertTrue(Arrays.asList(recipients).contains(recipient1));
    assertTrue(Arrays.asList(recipients).contains(recipient2));
    assertTrue(Arrays.asList(recipients).contains(recipient3));
  }

  @Test
  void testRecipientExtraction_inconsistenConfig() {
    final NotificationChannel mailChannel = new NotificationChannel();
    mailChannel.setRecipients(recipient1);
    stockAlertsConfig.setNotificationChannels(List.of(mailChannel));

    final var caughtException = assertThrows(IllegalStateException.class, () -> testee.getRecipients(appConfig));
    assertEquals("email channel was invoked but not found in configuration", caughtException.getMessage());
  }

  @Test
  void sendEmailHappyCase() throws MessagingException, IOException {
    final NotificationChannel mailChannel = new NotificationChannel();
    mailChannel.setRecipients(recipient2);
    mailChannel.setType(Channel.EMAIL.getValue());
    stockAlertsConfig.setNotificationChannels(List.of(mailChannel));

    final MimeMessage mimeMessage = getMockedMimeMessage();

    LocalDateTime persistedTs = LocalDateTime.of(2025, Month.JULY, 17, 12, 16, 24, 12);
    LocalDateTime updatedTs = LocalDateTime.of(2025, Month.AUGUST, 12, 9, 16, 17, 34);
    final Security persisted = new Security("ABC", 12.0, "CHF", persistedTs, "Switzerland");
    final Security latest = new Security("ABC", 13.0, "CHF", updatedTs, "Switzerland");

    final Alert testAlert = new Alert();
    testAlert.setNotification(Channel.EMAIL.getValue());
    testAlert.setThreshold(12.25);


    testee.send(testAlert, latest, persisted);


    verify(mailSender).send(mimeMessage);
    assertEquals(recipient2, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Threshold CHF 12.25 for ABC crossed", mimeMessage.getSubject());
    assertEquals("Price for ABC moved from CHF 12.0 dated on 2025-07-17 12:16 to CHF 13.0 dated on 2025-08-12 09:16\n" +
            "Data refers to stock exchange Switzerland.\n",
        mimeMessage.getContent().toString());
  }

  private MimeMessage getMockedMimeMessage() {
    final Session sessionMock = Mockito.mock(Session.class);
    when(sessionMock.getProperties()).thenReturn(new Properties());
    final MimeMessage mimeMessage = new MimeMessage(sessionMock);
    when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    return mimeMessage;
  }

  @Test
  void sendEmail_invalidAddress() {
    final NotificationChannel mailChannel = new NotificationChannel();
    mailChannel.setRecipients("i_am_NOT_a_valid_Email-Address");
    mailChannel.setType(Channel.EMAIL.getValue());
    stockAlertsConfig.setNotificationChannels(List.of(mailChannel));

    final Security persisted = new Security("ABC", 12.0, "CHF", LocalDateTime.now(), "Switzerland");
    final Security latest = new Security("ABC", 13.0, "CHF", LocalDateTime.now(), "Switzerland");

    final Alert testAlert = new Alert();
    testAlert.setNotification(Channel.EMAIL.getValue());

    final MailSendException runtimeException = assertThrows(MailSendException.class, () -> testee.send(testAlert, latest, persisted));

    verify(mailSender, never()).send(any(MimeMessage.class));
    assertEquals("Failed to send email to i_am_NOT_a_valid_Email-Address: Missing final '@domain'", runtimeException.getMessage());
  }
}