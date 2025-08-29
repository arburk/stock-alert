package com.github.arburk.stockalert.infrastructure.notification.email;

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
import org.springframework.test.util.ReflectionTestUtils;

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
  private EmailNotificationSender testee;

  private static final String RECIPIENT_1 = "me@here.com";
  private static final String RECIPIENT_2 = "myself@there.com";
  private static final String RECIPIENT_3 = "i@outa-space.com";


  @BeforeEach
  void setUp() {
    mailSender = Mockito.mock(JavaMailSender.class);
    testee = new EmailNotificationSender(mailSender);
  }

  @Test
  void verifyChannelSetting() {
    assertEquals(Channel.EMAIL, testee.getChannel());
  }

  @Test
  void testRecipientsExtraction() {
    final NotificationChannel mailChannel = new NotificationChannel(Channel.EMAIL.getValue(), RECIPIENT_1 + ", " + RECIPIENT_2 + " ; " + RECIPIENT_3, false, false);
    final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, null, null, List.of(mailChannel), List.of());
    final String[] recipients = testee.getRecipients(stockAlertsConfig);

    assertEquals(3, recipients.length);
    assertTrue(Arrays.asList(recipients).contains(RECIPIENT_1));
    assertTrue(Arrays.asList(recipients).contains(RECIPIENT_2));
    assertTrue(Arrays.asList(recipients).contains(RECIPIENT_3));
  }

  @Test
  void testRecipientExtraction_inconsistenConfig() {
    final NotificationChannel mailChannel = new NotificationChannel(null, RECIPIENT_1, false, false);
    final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, null, null, List.of(mailChannel), List.of());

    final var caughtException = assertThrows(IllegalStateException.class, () -> testee.getRecipients(stockAlertsConfig));
    assertEquals("email channel was invoked but not found in configuration", caughtException.getMessage());
  }

  @Test
  void sendEmailHappyCase() throws MessagingException, IOException {
    final NotificationChannel mailChannel = new NotificationChannel(Channel.EMAIL.getValue(), RECIPIENT_2, false,false);
    final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, null, null, List.of(mailChannel), List.of());
    final MimeMessage mimeMessage = getMockedMimeMessage();

    LocalDateTime persistedTs = LocalDateTime.of(2025, Month.JULY, 17, 12, 16, 24, 12);
    LocalDateTime updatedTs = LocalDateTime.of(2025, Month.AUGUST, 12, 9, 16, 17, 34);
    final Security persisted = new Security("ABC", 12.0, "CHF", null, persistedTs, "Switzerland");
    final Security latest = new Security("ABC", 13.0, "CHF", null, updatedTs, "Switzerland");
    final Alert testAlert = new Alert(12.25, Channel.EMAIL.getValue(), null);

    ReflectionTestUtils.setField(testee, "from", "mocked@example.com");

    testee.send(stockAlertsConfig, testAlert, latest, persisted);


    verify(mailSender).send(mimeMessage);
    assertEquals(RECIPIENT_2, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Threshold CHF 12.25 for ABC crossed", mimeMessage.getSubject());
    assertEquals("""
            Price for ABC moved from CHF 12.0 dated on 2025-07-17 12:16 to CHF 13.0 dated on 2025-08-12 09:16
            Data refers to stock exchange Switzerland.
            """,
        mimeMessage.getContent().toString());
  }

  @Test
  void sendPercentageEmailHappyCase() throws MessagingException, IOException {
    final NotificationChannel mailChannel = new NotificationChannel(Channel.EMAIL.getValue(), RECIPIENT_2, false,true);
    final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, null, "5%", List.of(mailChannel), List.of());
    final MimeMessage mimeMessage = getMockedMimeMessage();

    LocalDateTime persistedTs = LocalDateTime.of(2025, Month.JULY, 17, 12, 16, 24, 12);
    LocalDateTime updatedTs = LocalDateTime.of(2025, Month.AUGUST, 12, 9, 16, 17, 34);
    final Security persisted = new Security("ABC", 12.0, "CHF", null, persistedTs, "Switzerland");
    final Security latest = new Security("ABC", 12.64, "CHF", null, updatedTs, "Switzerland");

    ReflectionTestUtils.setField(testee, "from", "mocked@example.com");

    testee.send(stockAlertsConfig, latest, persisted, .05, 0.0527);


    verify(mailSender).send(mimeMessage);
    assertEquals(RECIPIENT_2, mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString());
    assertEquals("Threshold of 5.00 % crossed for ABC", mimeMessage.getSubject());
    assertEquals("""
            Price for ABC moved from CHF 12.0 dated on 2025-07-17 12:16 to CHF 12.64.
            Price change is 5.27 % while defined threshold is 5.00 %.
            Data refers to stock exchange Switzerland dated on 2025-08-12 09:16.
            """,
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
    final NotificationChannel mailChannel = new NotificationChannel(Channel.EMAIL.getValue(), "i_am_NOT_a_valid_Email-Address", false, false);
    final StockAlertsConfig stockAlertsConfig = new StockAlertsConfig(null, null, null, List.of(mailChannel), List.of());
    final Security persisted = new Security("ABC", 12.0, "CHF", null, LocalDateTime.now(), "Switzerland");
    final Security latest = new Security("ABC", 13.0, "CHF", null, LocalDateTime.now(), "Switzerland");
    final Alert testAlert = new Alert(0, Channel.EMAIL.getValue(), null);

    final MailSendException runtimeException = assertThrows(MailSendException.class, () -> testee.send(stockAlertsConfig, testAlert, latest, persisted));

    verify(mailSender, never()).send(any(MimeMessage.class));
    assertEquals("Failed to send email to i_am_NOT_a_valid_Email-Address: Missing final '@domain'", runtimeException.getMessage());
  }
}