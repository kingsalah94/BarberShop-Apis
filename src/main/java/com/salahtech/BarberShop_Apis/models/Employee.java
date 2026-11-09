package com.salahtech.BarberShop_Apis.models;

import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lien vers le compte utilisateur (pour qu'il se connecte)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private ApplicationUser user;

    // Il travaille dans quel salon ?
    @ManyToOne
    @JoinColumn(name = "salon_id")
    private Salon salon;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "barber_id", unique = true)
    private Barber barber;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole role;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "employee_salons",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "salon_id")
    )
    @Builder.Default
    private Set<Salon> salons = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_salon_id")
    private Salon primarySalon;

    // Est-il présent aujourd'hui ?
   // private boolean isAvailable;
    @Builder.Default
    private boolean isAvailable = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    // Couleur pour le calendrier (ex: #FF5733)
    private String calendarColor;

    // Compétences : Quelles catégories il sait faire ?
    // Ex: Ahmed sait faire "Barbe" (ID 1) mais pas "Coloration" (ID 4)
//    @ManyToMany
//    @JoinTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"), inverseJoinColumns = @JoinColumn(name = "category_id"))
//    private List<Category> skills;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private List<Category> skills = new ArrayList<>();


private LocalDate hireDate;

@Column(length = 1000)
private String notes;



@Column(name = "created_at", nullable = false, updatable = false)
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;

@PrePersist
protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    updatedAt = LocalDateTime.now();
}
}