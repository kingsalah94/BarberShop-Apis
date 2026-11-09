package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Config.CurrentUser;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.PostService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Post;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Portfolio (Posts)", description = "Gestion des photos des barbiers")
public class PostController {

    private final PostService postService;
    private final FileStorageService fileStorageService; // Injecté ici pour gérer le fichier physique

    @Operation(summary = "Ajouter une photo au portfolio (Barbier uniquement)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_BARBER')")
    public ResponseEntity<Post> addPost(
            @CurrentUser ApplicationUser user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        // Vérification de sécurité supplémentaire
        if (user.getBarber() == null) {
            return ResponseEntity.badRequest().build(); // ou throw AccessDeniedException
        }

        // 1. Gestion Technique : Upload du fichier physique
        String imageUrl = fileStorageService.storeFile(file);

        // 2. Gestion Métier : Appel du service avec l'URL et l'ID du barbier
        Post post = postService.addPost(user.getBarber().getId(), imageUrl, description);

        return ResponseEntity.ok(post);
    }

    @Operation(summary = "Voir le portfolio d'un barbier (Public)")
    @GetMapping("/barber/{barberId}")
    public ResponseEntity<List<Post>> getBarberPortfolio(@PathVariable Long barberId) {
        return ResponseEntity.ok(postService.getPostsByBarber(barberId));
    }

    @Operation(summary = "Supprimer une photo (Barbier propriétaire uniquement)")
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasAuthority('ROLE_BARBER')")
    public ResponseEntity<Void> deletePost(
            @CurrentUser ApplicationUser user,
            @PathVariable Long postId) {

        if (user.getBarber() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Le service vérifiera si le post appartient bien à ce barbier
        postService.deletePost(postId, user.getBarber().getId());
        
        // Optionnel : Tu pourrais aussi appeler fileStorageService.deleteFile(...) ici
        // si tu récupérais l'URL avant la suppression.

        return ResponseEntity.noContent().build();
    }
}