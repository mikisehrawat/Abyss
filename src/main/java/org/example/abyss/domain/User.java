package org.example.abyss.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue // Default UUID generation
    private java.util.UUID id;

    private String name;

    @Column(unique = true)
    private String email;

    private String imageUrl;

    private boolean enabled = true;

    private Long createdAt = System.currentTimeMillis();

    // --- NEW FIELD ---
    @Enumerated(EnumType.STRING) // Stores "USER" or "ADMIN" as text
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Identity> identities;
}