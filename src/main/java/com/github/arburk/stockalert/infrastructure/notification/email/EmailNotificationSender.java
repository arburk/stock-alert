package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.config.ApplicationConfig;
import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.Alert;
import com.github.arburk.stockalert.application.service.notification.Channel;
import com.github.arburk.stockalert.application.service.notification.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

  private final JavaMailSender mailSender;
  private final ApplicationConfig appConfig;

  @Value("${spring.mail.properties.mail.smtp.sender-address}")
  private String from;

  public EmailNotificationSender(final JavaMailSender mailSender, final ApplicationConfig appConfig) {
    this.mailSender = mailSender;
    this.appConfig = appConfig;
  }

  @Override
  public Channel getChannel() {
    return Channel.EMAIL;
  }

  @Override
  public void send(final Alert alert, final Security latest, final Security persisted) {
    sendEmail(renderSubject(alert, latest), renderMessage(latest, persisted));
  }

  private String renderMessage(final Security latest, final Security persisted) {
    String currency = latest.currency();
    return """
        Price for %s moved from %s %s dated on %s to %s %s dated on %s
        Data refers to stock exchange %s.
        """.formatted(
        latest.symbol(),
        currency, persisted.price(), persisted.getTimestampFormatted(),
        currency, latest.price(), latest.getTimestampFormatted(),
        getStockExchange(latest, persisted)
    );
  }

  private String getStockExchange(final Security latest, final Security persisted) {
    final String persitedOne = (persisted == null) ? null : persisted.exchange();
    if (latest.exchange() == null) {
      return persitedOne;
    }

    return latest.exchange().equals(persitedOne)
        ? persitedOne
        : latest.exchange() + "/" + persitedOne;
  }

  private String renderSubject(final Alert alert, final Security latest) {
    return "Threshold %s %s for %s crossed".formatted(
        latest.currency(), alert.getThreshold(), latest.symbol());
  }

  private void sendEmail(final String subject, String message) {
    final String[] recipient = getRecipients(appConfig);
    try {
      MimeMessage mail = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(mail, "UTF-8");
      helper.setValidateAddresses(true);
      helper.setTo(recipient);
      helper.setFrom(from);
      helper.setSubject(subject);
      helper.setText(message, false);
      mailSender.send(mail);
      log.debug("Email successfully send to {}", (Object[]) recipient);
    } catch (MessagingException e) {
      final String msg = "Failed to send email to %s: %s".formatted(
          String.join(", ", recipient), e.getMessage());
      throw new MailSendException(msg, e);
    }
  }

}
