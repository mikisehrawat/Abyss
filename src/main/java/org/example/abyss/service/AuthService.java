package org.example.abyss.service;

import lombok.RequiredArgsConstructor;
import org.example.abyss.domain.Identity;
import org.example.abyss.domain.ProviderType;
import org.example.abyss.domain.User;
import org.example.abyss.dto.AuthenticationRequest;
import org.example.abyss.dto.AuthenticationResponse;
import org.example.abyss.dto.RegisterRequest;
import org.example.abyss.repository.UserRepository;
import org.example.abyss.security.JwtService;
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
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

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
                .providerId("LOCAL_" + java.util.UUID.randomUUID().toString())
                .credential(passwordEncoder.encode(request.getPassword()))
                .user(user)
                .build();

        user.getIdentities().add(identity);
        repository.save(user);

        var springUser = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                identity.getCredential(),
                Collections.emptyList()
        );
        var jwtToken = jwtService.generateToken(springUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // 1. Verify username/password (This throws exception if invalid)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();

        String passwordHash = user.getIdentities().stream()
                .filter(i -> "LOCAL".equals(i.getProvider()))
                .findFirst()
                .map(Identity::getCredential)
                .orElse("");

        var springUser = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                passwordHash,
                Collections.emptyList()
        );

        var jwtToken = jwtService.generateToken(springUser);

        return AuthenticationResponse.builder()
                .token(jwtToken)
                .build();
    }
}