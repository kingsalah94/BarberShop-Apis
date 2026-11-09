package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.CatalogService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.SalonFavorite;
import com.salahtech.BarberShop_Apis.repositories.SalonFavoriteRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Salons favoris du client connecté. */
@RestController
@RequestMapping("/v1/me/favorite-salons")
@RequiredArgsConstructor
@Tag(name = "My Favorite Salons", description = "Salons favoris du client connecté")
public class MyFavoriteSalonsController {

    private final SalonFavoriteRepository favoriteRepository;
    private final SalonRepository salonRepository;
    private final CatalogService catalogService;

    @Operation(summary = "Mes salons favoris (fiches catalogue, le plus récent d'abord)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<SalonCatalogDto>> list(@CurrentUser ApplicationUser user) {
        List<Long> ids = favoriteRepository.findSalonIdsByClientId(client(user).getId());
        Map<Long, SalonCatalogDto> catalog = catalogService.listSalons().stream()
                .collect(Collectors.toMap(SalonCatalogDto::getId, Function.identity(), (a, b) -> a));
        return ResponseEntity.ok(ids.stream().map(catalog::get).filter(java.util.Objects::nonNull).toList());
    }

    @Operation(summary = "Identifiants de mes salons favoris (pour afficher les cœurs)")
    @GetMapping("/ids")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Long>> ids(@CurrentUser ApplicationUser user) {
        return ResponseEntity.ok(favoriteRepository.findSalonIdsByClientId(client(user).getId()));
    }

    @Operation(summary = "Ajouter un salon à mes favoris (idempotent)")
    @PutMapping("/{salonId}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Void> add(@PathVariable Long salonId, @CurrentUser ApplicationUser user) {
        Client c = client(user);
        if (!favoriteRepository.existsByClient_IdAndSalon_Id(c.getId(), salonId)) {
            favoriteRepository.save(SalonFavorite.builder()
                    .client(c)
                    .salon(salonRepository.findById(salonId)
                            .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId)))
                    .build());
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Retirer un salon de mes favoris (idempotent)")
    @DeleteMapping("/{salonId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(@PathVariable Long salonId, @CurrentUser ApplicationUser user) {
        favoriteRepository.deleteByClient_IdAndSalon_Id(client(user).getId(), salonId);
        return ResponseEntity.noContent().build();
    }

    private Client client(ApplicationUser user) {
        if (user == null || user.getClient() == null) {
            throw new BusinessException("Un profil client est requis pour gérer des favoris");
        }
        return user.getClient();
    }
}
