package org.example.abyss.controller;

import jakarta.servlet.http.HttpServletRequest; // <--- Make sure this is imported
import lombok.RequiredArgsConstructor;
import org.example.abyss.dto.AuthenticationRequest;
import org.example.abyss.dto.AuthenticationResponse;
import org.example.abyss.dto.RegisterRequest;
import org.example.abyss.service.AuthService;
import org.example.abyss.service.GithubAuthService;
import org.example.abyss.service.GoogleAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService service;
    private final GoogleAuthService googleAuthService;
    private final GithubAuthService githubAuthService;

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

    // --- THIS WAS MISSING ---
    @PostMapping("/refresh-token")
    public ResponseEntity<AuthenticationResponse> refreshToken(
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(service.refreshToken(request));
    }
    @GetMapping("/github/url")
    public ResponseEntity<String> getGithubUrl() {
        return ResponseEntity.ok(githubAuthService.getGithubLoginUrl());
    }

    @GetMapping("/callback/github")
    public ResponseEntity<AuthenticationResponse> handleGithubCallback(
            @RequestParam("code") String code
    ) {
        String accessToken = githubAuthService.getAccessToken(code);
        var githubUser = githubAuthService.getUserInfo(accessToken);
        return ResponseEntity.ok(service.authenticateGithub(githubUser));
    }
}