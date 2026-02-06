package com.salahtech.BarberShop_Apis.reppsitories;

import com.salahtech.BarberShop_Apis.models.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Récupérer toutes les catégories actives
    List<Category> findByIsActiveTrue();

    // Récupérer les catégories par type d'établissement (ex: pour afficher
    // seulement les trucs de coiffure)
    List<Category> findByEstablishmentType_CodeAndIsActiveTrue(String typeCode);
}