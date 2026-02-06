package com.salahtech.BarberShop_Apis.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Data
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers le compte utilisateur (pour qu'il se connecte)
    @OneToOne
    private ApplicationUser user;

    // Il travaille dans quel salon ?
    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;

    // Est-il présent aujourd'hui ?
    private boolean isAvailable;

    // Couleur pour le calendrier (ex: #FF5733)
    private String calendarColor;

    // Compétences : Quelles catégories il sait faire ?
    // Ex: Ahmed sait faire "Barbe" (ID 1) mais pas "Coloration" (ID 4)
    @ManyToMany
    @JoinTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<Category> skills;
}