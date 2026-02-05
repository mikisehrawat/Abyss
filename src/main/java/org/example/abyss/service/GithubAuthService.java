package org.example.abyss.service;

import lombok.RequiredArgsConstructor;
import org.example.abyss.dto.GoogleUserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GithubAuthService {

    @Value("${spring.security.oauth2.client.registration.github.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.github.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.github.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getGithubLoginUrl() {
        return "https://github.com/login/oauth/authorize?" +
                "client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=user:email"; // Request permission to see email
    }

    public String getAccessToken(String code) {
        String url = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Pass parameters in URL to avoid form-encoding issues
        String fullUrl = url + "?client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + code +
                "&redirect_uri=" + redirectUri;

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(fullUrl, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null || body.get("access_token") == null) {
                throw new RuntimeException("GitHub did not return a token. Check your Client Secret.");
            }

            return (String) body.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get access token from GitHub: " + e.getMessage());
        }
    }

    public GoogleUserDTO getUserInfo(String accessToken) {
        String url = "https://api.github.com/user";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> attributes = response.getBody();

            String githubId = String.valueOf(attributes.get("id"));
            String name = (String) attributes.get("name");
            String email = (String) attributes.get("email");
            String avatar = (String) attributes.get("avatar_url");

            if (name == null) name = (String) attributes.get("login");

            // --- FIX: IF EMAIL IS NULL, FETCH IT MANUALLY ---
            if (email == null) {
                email = fetchEmailFromGithub(accessToken);
            }

            return GoogleUserDTO.builder()
                    .sub(githubId)
                    .name(name)
                    .email(email)
                    .picture(avatar)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch user info: " + e.getMessage());
        }
    }

    // New Helper Method to get Private Emails
    private String fetchEmailFromGithub(String accessToken) {
        String url = "https://api.github.com/user/emails";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {}
        );

        List<Map<String, Object>> emails = response.getBody();
        if (emails != null) {
            for (Map<String, Object> emailObj : emails) {
                boolean primary = (Boolean) emailObj.get("primary");
                boolean verified = (Boolean) emailObj.get("verified");
                if (primary && verified) {
                    return (String) emailObj.get("email");
                }
            }
        }
        throw new RuntimeException("Could not find a primary, verified email for this GitHub account.");
    }
}