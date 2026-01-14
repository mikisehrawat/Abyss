package org.example.abyss;

import org.example.abyss.security.JwtService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;

@SpringBootApplication
public class AbyssApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbyssApplication.class, args);
    }

    // --- PASTE YOUR CODE HERE (Inside the class, but outside main) ---
    @Bean
    public CommandLineRunner testJwt(JwtService jwtService, org.example.abyss.repository.UserRepository userRepo) {
        return args -> {
            // 1. Create and Save a User to DB
            String email = "admin@abyss.com";
            if (!userRepo.existsByEmail(email)) {
                org.example.abyss.domain.User user = new org.example.abyss.domain.User();
                user.setEmail(email);
                user.setName("Admin");
                userRepo.save(user);
            }

            // 2. Generate Token for this user
            // We need a UserDetails object for the token generation
            org.springframework.security.core.userdetails.User springUser =
                    new org.springframework.security.core.userdetails.User(email, "", new ArrayList<>());

            String token = jwtService.generateToken(springUser);

            System.out.println("------------------------------------------------");
            System.out.println("ACCESS TOKEN: " + token);
            System.out.println("------------------------------------------------");
        };
    }
}