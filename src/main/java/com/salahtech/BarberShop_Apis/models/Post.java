package com.salahtech.BarberShop_Apis.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Post {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl; // L'URL de stockage (ex: /uploads/abc.jpg)

    private String description;

    private Integer likes = 0; // Pour un futur système de "J'aime"

    private LocalDateTime createdAt;

    // Relation : Plusieurs Posts appartiennent à un seul Barbier
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", nullable = false)
    @JsonIgnore // ⚠️ Important : Évite la boucle infinie JSON (Post -> Barber -> Post...)
    private Barber barber;

    // Méthode utilitaire pour initialiser la date avant insertion
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.likes == null)
            this.likes = 0;
    }
}