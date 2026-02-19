package com.zingo.app.repository;

import com.zingo.app.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);

  @Query("select u, p from User u join Profile p on p.userId = u.id where u.email = :email")
  Optional<Object[]> findUserWithProfileByEmail(@Param("email") String email);

  @Query("select u, p from User u join Profile p on p.userId = u.id where u.id = :userId")
  Optional<Object[]> findUserWithProfileById(@Param("userId") Long userId);
}
