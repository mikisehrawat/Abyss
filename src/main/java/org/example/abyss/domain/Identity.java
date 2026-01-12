package org.example.abyss.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "identities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //"I am linking this item to a parent, but do not load the parent's data from the database unless I explicitly ask for it."
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProviderType provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(name = "credential")
    private String credential;
}