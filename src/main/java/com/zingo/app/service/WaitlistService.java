package com.zingo.app.service;

import com.zingo.app.dto.WaitlistDtos.JoinWaitlistRequest;
import com.zingo.app.dto.WaitlistDtos.JoinWaitlistResponse;
import com.zingo.app.entity.WaitlistSignup;
import com.zingo.app.repository.WaitlistSignupRepository;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitlistService {
  private final WaitlistSignupRepository repo;

  public WaitlistService(WaitlistSignupRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public JoinWaitlistResponse join(JoinWaitlistRequest request, String ip, String userAgent) {
    // Honeypot: accept but do not store.
    if (request.company() != null && !request.company().isBlank()) {
      return new JoinWaitlistResponse(true, "joined", "You’re on the waitlist. Welcome.");
    }

    String email = request.email().trim().toLowerCase();
    String name = request.name() == null ? null : request.name().trim();
    if (name != null && name.isBlank()) name = null;

    // Fast path: already exists.
    if (repo.findByEmail(email).isPresent()) {
      return new JoinWaitlistResponse(true, "already", "You’re already on the waitlist.");
    }

    WaitlistSignup signup = new WaitlistSignup();
    signup.setEmail(email);
    signup.setName(name);
    signup.setIp(ip);
    signup.setUserAgent(userAgent);
    signup.setCreatedAt(Instant.now());

    try {
      repo.save(signup);
      return new JoinWaitlistResponse(true, "joined", "You’re on the waitlist. Welcome.");
    } catch (DataIntegrityViolationException e) {
      // Unique constraint race: treat as already joined.
      return new JoinWaitlistResponse(true, "already", "You’re already on the waitlist.");
    }
  }
}

