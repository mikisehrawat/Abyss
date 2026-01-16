package org.example.abyss.controller;

import lombok.RequiredArgsConstructor;
import org.example.abyss.dto.AuthenticationRequest;
import org.example.abyss.dto.AuthenticationResponse;
import org.example.abyss.dto.RegisterRequest;
import org.example.abyss.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.abyss.service.GoogleAuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(service.register(request));
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }

    private final GoogleAuthService googleAuthService;

    @GetMapping("/google/url")
    public ResponseEntity<String> getGoogleUrl() {
        return ResponseEntity.ok(googleAuthService.getGoogleLoginUrl());
    }

    @GetMapping("/callback/google")
    public ResponseEntity<AuthenticationResponse> handleGoogleCallback(
            @RequestParam("code") String code
    ) {
        String accessToken = googleAuthService.getAccessToken(code);

        var googleUser = googleAuthService.getUserInfo(accessToken);

        return ResponseEntity.ok(service.authenticateGoogle(googleUser));
    }
}