package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.LoyaltyStatusDto;
import com.salahtech.BarberShop_Apis.Services.Implementations.LoyaltyService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Cartes de fidélité du client connecté (10 visites payées = 1 prestation offerte, par salon). */
@RestController
@RequestMapping("/v1/me/loyalty")
@RequiredArgsConstructor
@Tag(name = "My Loyalty", description = "Fidélité du client connecté")
public class MyLoyaltyController {

    private final LoyaltyService loyaltyService;

    @Operation(summary = "Mes cartes de fidélité par salon")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LoyaltyStatusDto>> mine(@CurrentUser ApplicationUser user) {
        if (user == null || user.getClient() == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(loyaltyService.mine(user.getClient().getId()));
    }
}
