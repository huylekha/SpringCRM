package com.company.platform.auth.auth.controller;

import com.company.platform.auth.auth.dto.request.LoginRequest;
import com.company.platform.auth.auth.dto.request.LogoutRequest;
import com.company.platform.auth.auth.dto.request.RefreshRequest;
import com.company.platform.auth.auth.dto.response.LoginResponse;
import com.company.platform.auth.auth.dto.response.MessageResponse;
import com.company.platform.auth.auth.dto.response.RefreshResponse;
import com.company.platform.auth.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthenticationService authenticationService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authenticationService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
    return ResponseEntity.ok(authenticationService.refresh(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<MessageResponse> logout(@Valid @RequestBody LogoutRequest request) {
    authenticationService.logout(request);
    return ResponseEntity.ok(new MessageResponse("Logout successful."));
  }
}
