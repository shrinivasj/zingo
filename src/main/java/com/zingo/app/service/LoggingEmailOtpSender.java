package com.zingo.app.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailOtpSender implements EmailOtpSender {
  private static final Logger log = LoggerFactory.getLogger(LoggingEmailOtpSender.class);

  private final JavaMailSender mailSender;
  private final String fromEmail;
  private final String fromName;
  private final boolean smtpEnabled;

  public LoggingEmailOtpSender(
      ObjectProvider<JavaMailSender> mailSenderProvider,
      @Value("${app.auth.otp.mail.fromEmail:no-reply@aurofly.local}") String fromEmail,
      @Value("${app.auth.otp.mail.fromName:Aurofly}") String fromName,
      @Value("${app.auth.otp.mail.smtpEnabled:false}") boolean smtpEnabled) {
    this.mailSender = mailSenderProvider.getIfAvailable();
    this.fromEmail = fromEmail;
    this.fromName = fromName;
    this.smtpEnabled = smtpEnabled;
  }

  @Override
  public void sendLoginCode(String email, String code, long expiresInMinutes) {
    if (smtpEnabled && mailSender != null) {
      try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setTo(email);
        helper.setFrom(fromEmail, fromName);
        helper.setSubject("Your Aurofly login code");
        helper.setText(buildEmailBody(code, expiresInMinutes), false);
        mailSender.send(message);
        log.info("Sent OTP email to {}", email);
        return;
      } catch (Exception ex) {
        log.error("Failed to send OTP email to {}. Falling back to log-only sender", email, ex);
      }
    }

    log.info("Email OTP for {} is {} (expires in {} minutes)", email, code, expiresInMinutes);
  }

  private String buildEmailBody(String code, long expiresInMinutes) {
    return "Your Aurofly login code is " + code + ". It expires in " + expiresInMinutes
        + " minutes. If you did not request this, you can ignore this email.";
  }
}
