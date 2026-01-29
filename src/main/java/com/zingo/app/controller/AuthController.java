package com.zingo.app.controller;

import com.zingo.app.dto.AuthDtos.AuthResponse;
import com.zingo.app.dto.AuthDtos.LoginRequest;
import com.zingo.app.dto.AuthDtos.RegisterRequest;
import com.zingo.app.dto.AuthDtos.UserDto;
import com.zingo.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public UserDto me() {
    return authService.me();
  }
}
