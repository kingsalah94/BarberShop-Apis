package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Services.Interfaces.CategoryService;
import com.salahtech.BarberShop_Apis.models.Category;
import com.salahtech.BarberShop_Apis.repositories.CategoryRepository;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @Override
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec l'ID: " + id));
    }
}