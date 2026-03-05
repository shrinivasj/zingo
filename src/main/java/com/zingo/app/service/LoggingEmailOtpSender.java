package com.zingo.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LoggingEmailOtpSender implements EmailOtpSender {
  private static final Logger log = LoggerFactory.getLogger(LoggingEmailOtpSender.class);

  private final String fromEmail;
  private final String fromName;
  private final boolean smtpEnabled;

  public LoggingEmailOtpSender(
      @Value("${app.auth.otp.mail.fromEmail:no-reply@aurofly.local}") String fromEmail,
      @Value("${app.auth.otp.mail.fromName:Aurofly}") String fromName,
      @Value("${app.auth.otp.mail.smtpEnabled:false}") boolean smtpEnabled) {
    this.fromEmail = fromEmail;
    this.fromName = fromName;
    this.smtpEnabled = smtpEnabled;
  }

  @Override
  public void sendLoginCode(String email, String code, long expiresInMinutes) {
    if (smtpEnabled) {
      // SMTP sending is intentionally not wired in this lightweight sender.
      // This keeps the backend bootable even when Spring Mail isn't on the classpath.
      log.warn(
          "SMTP is enabled but no SMTP sender is configured. Falling back to log-only OTP delivery. fromEmail={}, fromName={}",
          fromEmail,
          fromName);
    }

    log.info("Email OTP for {} is {} (expires in {} minutes)", email, code, expiresInMinutes);
  }

  // Intentionally no SMTP implementation here.
}
