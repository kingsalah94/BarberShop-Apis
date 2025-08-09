package com.salahtech.BarberShop_Apis.Web.Api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import static com.salahtech.BarberShop_Apis.Utils.Constants.*;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Auth.LoginRequest;

@Tag(name = "User", description = "API pour l'authentification et l'inscription")
public interface AuthApi {

    // register
    @PostMapping(value = APP_ROOT + "/user/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enregistrer un Client", description = "Cette méthode permet d'enregistrer un client")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur enregistré avec succès"),
        @ApiResponse(responseCode = "400", description = "Données invalides")
    })
    ApplicationUser register(@RequestBody LoginRequest request);

    // login
    @PostMapping(value = APP_ROOT + "/user/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Authentifier un utilisateur", description = "Retourne un token JWT")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentification réussie"),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides")
    })
    String login(@RequestBody LoginRequest request);

    // logout
    @PostMapping(value = APP_ROOT + "/user/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Déconnexion", description = "Déconnecte l'utilisateur en vidant le contexte de sécurité")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Déconnexion réussie")
    })
    void logout();

    // getCurrentUser
    @GetMapping(value = APP_ROOT + "/user/getCurrentUser", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Récupérer l'utilisateur actuel", description = "Retourne les informations de l'utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil utilisateur récupéré")
    })
    ApplicationUserDto getCurrentUser();

    // updateUser
    @PutMapping(value = APP_ROOT + "/user/updateUser", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Mettre à jour le profil", description = "Met à jour les informations du profil utilisateur connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil mis à jour")
    })
    ApplicationUserDto updateUser(@RequestBody ApplicationUserDto dto);

    // deleteCurrentUser

    @DeleteMapping(value = APP_ROOT + "/user/deleteCurrentUser")
    @Operation(summary = "Supprimer son compte", description = "Supprime définitivement le compte connecté")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Compte supprimé avec succès")
    })
    void deleteCurrentUser();


}
