package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.Interfaces.CategoryService;
import com.salahtech.BarberShop_Apis.models.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Gestion des catégories de services (Dynamique)")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Lister toutes les catégories actives")
    @GetMapping
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllActiveCategories());
    }
}