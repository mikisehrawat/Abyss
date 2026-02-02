package org.example.abyss;

import org.example.abyss.domain.ProviderType;
import org.example.abyss.service.AuthService;
import org.example.abyss.service.GoogleAuthService;
import org.example.abyss.dto.GoogleUserDTO;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AbyssApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbyssApplication.class, args);
    }
}