package com.zingo.app.repository;

import com.zingo.app.entity.EmailOtpChallenge;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOtpChallengeRepository extends JpaRepository<EmailOtpChallenge, Long> {
  Optional<EmailOtpChallenge> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, String purpose);
}
