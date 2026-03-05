package com.zingo.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zingo.app.exception.BadRequestException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenVerifier {
  private static final String CERT_URL =
      "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

  private final String projectId;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final AtomicReference<KeyCache> keyCacheRef =
      new AtomicReference<>(new KeyCache(Collections.emptyMap(), Instant.EPOCH));

  public FirebaseTokenVerifier(
      @Value("${app.auth.firebase.projectId}") String projectId,
      ObjectMapper objectMapper) {
    this.projectId = projectId;
    this.objectMapper = objectMapper;
    this.httpClient = HttpClient.newHttpClient();
  }

  public VerifiedFirebaseToken verify(String idToken) {
    try {
      Jws<Claims> jwt = Jwts.parserBuilder()
          .setSigningKeyResolver(new SigningKeyResolverAdapter() {
            @Override
            public Key resolveSigningKey(Header header, Claims claims) {
              String keyId = header.getKeyId();
              if (keyId == null || keyId.isBlank()) {
                throw new BadRequestException("Missing Firebase token key ID");
              }
              return getPublicKey(keyId);
            }
          })
          .build()
          .parseClaimsJws(idToken);

      Claims claims = jwt.getBody();
      validateClaims(claims);

      String email = claims.get("email", String.class);
      Boolean emailVerified = claims.get("email_verified", Boolean.class);
      String name = claims.get("name", String.class);
      String picture = claims.get("picture", String.class);
      String provider = extractProvider(claims);

      if (email == null || email.isBlank()) {
        throw new BadRequestException("Firebase token does not include an email address");
      }
      if (!Boolean.TRUE.equals(emailVerified)) {
        throw new BadRequestException("Firebase account email is not verified");
      }

      return new VerifiedFirebaseToken(
          claims.getSubject(),
          email,
          name,
          picture,
          provider);
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("Invalid Firebase ID token");
    }
  }

  private void validateClaims(Claims claims) {
    Date now = new Date();
    if (claims.getExpiration() == null || !claims.getExpiration().after(now)) {
      throw new BadRequestException("Firebase token has expired");
    }
    if (claims.getIssuedAt() == null || claims.getIssuedAt().after(now)) {
      throw new BadRequestException("Firebase token issued-at time is invalid");
    }
    String audience = claims.getAudience();
    if (!projectId.equals(audience)) {
      throw new BadRequestException("Firebase token audience is invalid");
    }
    String issuer = claims.getIssuer();
    if (!("https://securetoken.google.com/" + projectId).equals(issuer)) {
      throw new BadRequestException("Firebase token issuer is invalid");
    }
    String subject = claims.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new BadRequestException("Firebase token subject is invalid");
    }
    Date authTime = toDate(claims.get("auth_time"));
    if (authTime != null && authTime.after(now)) {
      throw new BadRequestException("Firebase token auth time is invalid");
    }
  }

  @SuppressWarnings("unchecked")
  private String extractProvider(Claims claims) {
    Object firebaseClaim = claims.get("firebase");
    if (!(firebaseClaim instanceof Map<?, ?> firebaseMap)) {
      return null;
    }
    Object provider = firebaseMap.get("sign_in_provider");
    return provider instanceof String providerString ? providerString : null;
  }

  private Key getPublicKey(String keyId) {
    KeyCache cache = keyCacheRef.get();
    Instant now = Instant.now();
    if (cache.expiresAt().isAfter(now) && cache.keys().containsKey(keyId)) {
      return cache.keys().get(keyId);
    }

    synchronized (keyCacheRef) {
      cache = keyCacheRef.get();
      now = Instant.now();
      if (cache.expiresAt().isAfter(now) && cache.keys().containsKey(keyId)) {
        return cache.keys().get(keyId);
      }
      KeyCache refreshed = refreshKeys();
      keyCacheRef.set(refreshed);
      Key key = refreshed.keys().get(keyId);
      if (key == null) {
        throw new BadRequestException("Firebase signing key not found");
      }
      return key;
    }
  }

  private KeyCache refreshKeys() {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(CERT_URL)).GET().build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new BadRequestException("Failed to fetch Firebase signing certificates");
      }
      Map<String, String> certificates = objectMapper.readValue(
          response.body(),
          new TypeReference<Map<String, String>>() {});
      Map<String, Key> keys = certificates.entrySet().stream()
          .collect(java.util.stream.Collectors.toUnmodifiableMap(
              Map.Entry::getKey,
              entry -> parseCertificate(entry.getValue()).getPublicKey()));
      long maxAgeSeconds = extractMaxAgeSeconds(response.headers().firstValue("Cache-Control").orElse(""));
      return new KeyCache(keys, Instant.now().plusSeconds(maxAgeSeconds));
    } catch (BadRequestException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new BadRequestException("Failed to verify Firebase signing certificates");
    }
  }

  private X509Certificate parseCertificate(String pem) {
    try {
      String body = pem
          .replace("-----BEGIN CERTIFICATE-----", "")
          .replace("-----END CERTIFICATE-----", "")
          .replaceAll("\\s+", "");
      byte[] der = Base64.getDecoder().decode(body);
      CertificateFactory factory = CertificateFactory.getInstance("X.509");
      return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der));
    } catch (Exception ex) {
      throw new BadRequestException("Invalid Firebase signing certificate");
    }
  }

  private long extractMaxAgeSeconds(String cacheControl) {
    for (String token : cacheControl.split(",")) {
      String trimmed = token.trim();
      if (trimmed.startsWith("max-age=")) {
        try {
          return Long.parseLong(trimmed.substring("max-age=".length()));
        } catch (NumberFormatException ignored) {
          break;
        }
      }
    }
    return 3600;
  }

  private Date toDate(Object value) {
    if (value instanceof Date date) {
      return date;
    }
    if (value instanceof Number number) {
      return new Date(number.longValue() * 1000L);
    }
    return null;
  }

  public record VerifiedFirebaseToken(
      String uid,
      String email,
      String name,
      String picture,
      String provider) {}

  private record KeyCache(Map<String, Key> keys, Instant expiresAt) {}
}
