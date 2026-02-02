package org.example.abyss.service;

import lombok.RequiredArgsConstructor;
import org.example.abyss.dto.GoogleUserDTO; // <--- IMPORTANT: Using the shared DTO
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    // 1. GENERATE LOGIN URL
    public String getGoogleLoginUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=email%20profile";
    }

    // 2. EXCHANGE CODE FOR TOKEN
    public String getAccessToken(String code) {
        String url = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get access token from Google: " + e.getMessage());
        }
    }

    // 3. GET USER INFO (Updated to use Shared DTO)
    public GoogleUserDTO getUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            // Now strictly uses org.example.abyss.dto.GoogleUserDTO
            ResponseEntity<GoogleUserDTO> response = restTemplate.exchange(url, HttpMethod.GET, entity, GoogleUserDTO.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info from Google");
        }
    }

    // REMOVED: The inner "static class GoogleUserDTO" is deleted.
}