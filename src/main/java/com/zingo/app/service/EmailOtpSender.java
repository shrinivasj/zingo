package com.zingo.app.service;

public interface EmailOtpSender {
  void sendLoginCode(String email, String code, long expiresInMinutes);
}
