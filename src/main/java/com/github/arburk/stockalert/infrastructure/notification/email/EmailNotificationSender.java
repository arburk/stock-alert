package com.github.arburk.stockalert.infrastructure.notification.email;

import com.github.arburk.stockalert.application.domain.Security;
import com.github.arburk.stockalert.application.domain.config.AlertConfig;
import com.github.arburk.stockalert.application.domain.config.SecurityConfig;
import com.github.arburk.stockalert.application.domain.config.StockAlertsConfig;
import com.github.arburk.stockalert.application.service.notification.Channel;
import com.github.arburk.stockalert.application.service.notification.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.properties.mail.smtp.sender-address}")
  private String from;

  public EmailNotificationSender(final JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public Channel getChannel() {
    return Channel.EMAIL;
  }

  @Override
  public void send(final StockAlertsConfig stockAlertsConfig, final AlertConfig alertConfig, final Security latest, final Security persisted) {
    String currency = latest.currency();
    final String message = """
        Price for %s moved to %s %s dated on %s - from formerly %s %s dated on %s
        %s
        Data refers to stock exchange %s.
        """.formatted(
        latest.symbol(),
        currency, latest.price(), latest.getTimestampFormatted(),
        currency, persisted.price(), persisted.getTimestampFormatted(),
        renderComment(alertConfig, stockAlertsConfig.findConfig(latest)),
        getStockExchange(latest, persisted)
    );
    final String subject = "Threshold %s %s for %s crossed".formatted(
        latest.currency(), alertConfig.threshold(), latest.symbol());
    sendEmail(subject, message, stockAlertsConfig);
  }

  private String renderComment(final AlertConfig alertConfig, final SecurityConfig stockAlertsConfig) {
    String result = "";
    if (stockAlertsConfig != null && StringUtils.isNotBlank(stockAlertsConfig._comment())) {
      result += stockAlertsConfig._comment().trim();
    }

    if (alertConfig != null && StringUtils.isNotBlank(alertConfig._comment())) {
      if (!result.isEmpty()) {
        result += " | ";
      }
      result += alertConfig._comment().trim();
    }
    return result.trim();
  }

  @Override
  public void send(final StockAlertsConfig stockAlertsConfig, final Security latest, final Security persisted, final Double threshold, final double deviation) {
    String currency = latest.currency();
    final String message = """
        Price for %s moved to %s %s - from formerly %s %s dated on %s.
        Price change is %s while defined threshold is %s.
        Data refers to stock exchange %s dated on %s.
        """.formatted(
        latest.symbol(),
        currency, latest.price(),
        currency, persisted.price(), persisted.getTimestampFormatted(),
        Security.formatPercentage(deviation), Security.formatPercentage(threshold),
        getStockExchange(latest, persisted), latest.getTimestampFormatted()
    );
    final String subject = "Threshold of %s crossed for %s".formatted(
        Security.formatPercentage(threshold), latest.symbol());
    sendEmail(subject, message, stockAlertsConfig);
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

  private void sendEmail(final String subject, final String message, final StockAlertsConfig stockAlertsConfig) {
    final String[] recipient = getRecipients(stockAlertsConfig);
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
