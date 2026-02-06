package org.example.abyss.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.abyss.domain.*;
import org.example.abyss.dto.AuthenticationRequest;
import org.example.abyss.dto.AuthenticationResponse;
import org.example.abyss.dto.GoogleUserDTO;
import org.example.abyss.dto.RegisterRequest;
import org.example.abyss.repository.TokenRepository;
import org.example.abyss.repository.UserRepository;
import org.example.abyss.security.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // --- 1. REGISTER ---
    public AuthenticationResponse register(RegisterRequest request) {
        if (repository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists");
        }

        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .role(Role.USER) // Default Role
                .identities(new ArrayList<>())
                .build();

        var identity = Identity.builder()
                .provider(ProviderType.LOCAL)
                .providerId("LOCAL_" + java.util.UUID.randomUUID())
                .credential(passwordEncoder.encode(request.getPassword()))
                .user(user)
                .build();

        user.getIdentities().add(identity);
        var savedUser = repository.save(user);

        // FIX: Use helper to bake ROLE into the token
        var springUser = createSpringUser(savedUser);
        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        saveUserToken(savedUser, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    // --- 2. AUTHENTICATE (LOGIN) ---
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = repository.findByEmail(request.getEmail()).orElseThrow();

        // FIX: Use helper for consistency
        var springUser = createSpringUser(user);
        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    // --- 3a. GOOGLE AUTH ---
    public AuthenticationResponse authenticateGoogle(GoogleUserDTO googleUser) {
        User user = repository.findByEmail(googleUser.getEmail())
                .orElseGet(() -> {
                    var newUser = User.builder()
                            .role(Role.USER)
                            .name(googleUser.getName())
                            .email(googleUser.getEmail())
                            .imageUrl(googleUser.getPicture())
                            .identities(new ArrayList<>())
                            .build();
                    return repository.save(newUser);
                });

        boolean hasGoogleIdentity = user.getIdentities().stream()
                .anyMatch(i -> ProviderType.GOOGLE.equals(i.getProvider()));

        if (!hasGoogleIdentity) {
            var identity = Identity.builder()
                    .provider(ProviderType.GOOGLE)
                    .providerId(googleUser.getSub())
                    .user(user)
                    .build();
            user.getIdentities().add(identity);
            repository.save(user);
        }

        // FIX: Use helper
        var springUser = createSpringUser(user);
        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    // --- 3b. GITHUB AUTH ---
    public AuthenticationResponse authenticateGithub(GoogleUserDTO githubUser) {
        User user = repository.findByEmail(githubUser.getEmail())
                .orElseGet(() -> {
                    var newUser = User.builder()
                            .role(Role.USER)
                            .name(githubUser.getName())
                            .email(githubUser.getEmail())
                            .imageUrl(githubUser.getPicture())
                            .identities(new ArrayList<>())
                            .build();
                    return repository.save(newUser);
                });

        boolean hasGithubIdentity = user.getIdentities().stream()
                .anyMatch(i -> ProviderType.GITHUB.equals(i.getProvider()));

        if (!hasGithubIdentity) {
            var identity = Identity.builder()
                    .provider(ProviderType.GITHUB)
                    .providerId(githubUser.getSub())
                    .user(user)
                    .build();
            user.getIdentities().add(identity);
            repository.save(user);
        }

        // FIX: Use helper
        var springUser = createSpringUser(user);
        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    // --- 4. REFRESH TOKEN ---
    public AuthenticationResponse refreshToken(HttpServletRequest request) {
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        final String refreshToken;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        refreshToken = authHeader.substring(7);
        userEmail = jwtService.extractUsername(refreshToken);

        if (userEmail != null) {
            var user = this.repository.findByEmail(userEmail)
                    .orElseThrow();

            // FIX: Validate using the role-aware Spring User
            if (jwtService.isTokenValid(refreshToken, createSpringUser(user))) {

                var accessToken = jwtService.generateToken(createSpringUser(user));

                revokeAllUserTokens(user);
                saveUserToken(user, accessToken);

                return AuthenticationResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .build();
            }
        }
        return null;
    }

    // --- HELPER METHODS ---

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        var validUserTokens = tokenRepository.findAllValidTokenByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepository.saveAll(validUserTokens);
    }

    // --- CENTRALIZED USER CREATION ---
    private org.springframework.security.core.userdetails.User createSpringUser(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                "", // Password not needed for token generation
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}