package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.models.Category;
import java.util.List;

public interface CategoryService {
    List<Category> getAllActiveCategories();

    Category findById(Long id);
    // Tu pourras ajouter create/update ici plus tard pour l'admin
}