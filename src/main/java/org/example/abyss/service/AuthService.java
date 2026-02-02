package org.example.abyss.service;

import jakarta.servlet.http.HttpServletRequest; // <--- Added this
import lombok.RequiredArgsConstructor;
import org.example.abyss.domain.Identity;
import org.example.abyss.domain.ProviderType;
import org.example.abyss.domain.Token;
import org.example.abyss.domain.TokenType;
import org.example.abyss.domain.User;
import org.example.abyss.dto.AuthenticationRequest;
import org.example.abyss.dto.AuthenticationResponse;
import org.example.abyss.dto.GoogleUserDTO;
import org.example.abyss.dto.RegisterRequest;
import org.example.abyss.repository.TokenRepository;
import org.example.abyss.repository.UserRepository;
import org.example.abyss.security.JwtService;
import org.springframework.http.HttpHeaders; // <--- Added this
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;

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

        // Generate Tokens
        var springUser = new org.springframework.security.core.userdetails.User(user.getEmail(), identity.getCredential(), Collections.emptyList());
        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        // Save Token to DB
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

        // Find credential for Spring User
        String passwordHash = user.getIdentities().stream()
                .filter(i -> ProviderType.LOCAL.equals(i.getProvider()))
                .findFirst().map(Identity::getCredential).orElse("");

        var springUser = new org.springframework.security.core.userdetails.User(user.getEmail(), passwordHash, Collections.emptyList());

        var jwtToken = jwtService.generateToken(springUser);
        var refreshToken = jwtService.generateRefreshToken(springUser);

        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshToken)
                .build();
    }

    // --- 3. GOOGLE AUTH ---
    public AuthenticationResponse authenticateGoogle(GoogleUserDTO googleUser) {
        User user = repository.findByEmail(googleUser.getEmail())
                .orElseGet(() -> {
                    var newUser = User.builder()
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

        var springUser = new org.springframework.security.core.userdetails.User(user.getEmail(), "", Collections.emptyList());
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

            if (jwtService.isTokenValid(refreshToken, new org.springframework.security.core.userdetails.User(user.getEmail(), "", Collections.emptyList()))) {

                var accessToken = jwtService.generateToken(new org.springframework.security.core.userdetails.User(user.getEmail(), "", Collections.emptyList()));

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
}