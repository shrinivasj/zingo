package com.zingo.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final Key key;
  private final long expirationMinutes;

  public JwtService(@Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expirationMinutes}") long expirationMinutes) {
    byte[] keyBytes = toKeyBytes(secret);
    this.key = Keys.hmacShaKeyFor(keyBytes);
    this.expirationMinutes = expirationMinutes;
  }

  private byte[] toKeyBytes(String secret) {
    byte[] raw = secret.getBytes();
    if (raw.length < 32) {
      byte[] padded = new byte[32];
      for (int i = 0; i < padded.length; i++) {
        padded[i] = raw[i % raw.length];
      }
      raw = padded;
    }
    String base64 = java.util.Base64.getEncoder().encodeToString(raw);
    return Decoders.BASE64.decode(base64);
  }

  public String generateToken(Long userId, String email) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expirationMinutes * 60);
    return Jwts.builder()
        .setSubject(String.valueOf(userId))
        .claim("email", email)
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public JwtUser parseToken(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    Long userId = Long.parseLong(claims.getSubject());
    String email = claims.get("email", String.class);
    return new JwtUser(userId, email);
  }

  public record JwtUser(Long userId, String email) {}
}
