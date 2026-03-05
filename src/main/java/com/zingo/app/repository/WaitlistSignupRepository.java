package com.zingo.app.repository;

import com.zingo.app.entity.WaitlistSignup;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistSignupRepository extends JpaRepository<WaitlistSignup, Long> {
  Optional<WaitlistSignup> findByEmail(String email);
}

