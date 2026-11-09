package com.salahtech.BarberShop_Apis.Services;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.CompleteProfileRequest;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest.BarberProfileData;
import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserProfileService {

    private final ApplicationUserRepository userRepository;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;
    private final RoleRepository roleRepository;

    /**
     * Récupère le profil complet d'un utilisateur
     */
    public UserProfileDto getCompleteProfile(String email) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        return UserProfileDto.builder()
                .user(ApplicationUserDto.fromEntity(user))
                .client(user.getClient() != null ? ClientDto.fromEntity(user.getClient()) : null)
                .barber(user.getBarber() != null ? BarberDto.fromEntity(user.getBarber()) : null)
                .profileComplete(isProfileComplete(user))
                .build();
    }

    /**
     * Complète le profil après l'inscription
     */
    public UserProfileDto completeProfile(String email, CompleteProfileRequest request) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Mise à jour des infos complémentaires
        if (request.getPhone() != null)
            user.setPhone(request.getPhone());
        if (request.getProfileImageUrl() != null)
            user.setProfileImageUrl(request.getProfileImageUrl());

        // Créer ou mettre à jour le profil métier
        switch (user.getUserType()) {
            case CLIENT -> updateClientProfile(user, request);
            case BARBER -> updateBarberProfile(user, request);
            // SALON_OWNER : pas de profil barbier — seules les infos communes
            // (téléphone/photo) sont mises à jour ci-dessus ; le salon est géré à part.
            case SALON_OWNER -> { /* no-op */ }
            default -> { /* no-op */ }
        }

        user = userRepository.save(user);
        return getCompleteProfile(email);
    }

    /**
     * Permet à un client de devenir barbier
     */
    public UserProfileDto upgradeToBarber(String email, BarberProfileData barberData) {
        ApplicationUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (user.getUserType() != ApplicationUserType.CLIENT) {
            throw new InvalideEntityException("Seul un CLIENT peut devenir BARBER");
        }

        if (barberRepository.existsByUser_Id(user.getId())) {
            throw new ConflictException("Cet utilisateur a déjà un profil barbier");
        }

        // Changer le type et le rôle
        user.setUserType(ApplicationUserType.BARBER);

        Role barberRole = roleRepository.findByName("BARBER")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle BARBER non trouvé"));
        user.setRoles(Set.of(barberRole));

        // Créer le profil barbier
        Barber barber = Barber.builder()
                .user(user)
                .name(user.getFullName())
                .salonName(barberData.getSalonName())
                .location(barberData.getLocation())
                .latitude(barberData.getLatitude())
                .longitude(barberData.getLongitude())
                .description(barberData.getDescription())
                .specialties(barberData.getSpecialties())
                .priceFrom(barberData.getPriceFrom())
                .workingHours(barberData.getWorkingHours())
                .available(false) // Doit configurer ses disponibilités
                .rating(BigDecimal.ZERO)
                .reviewsCount(0)
                .build();

        barber = barberRepository.save(barber);
        user.setBarber(barber);
        user = userRepository.save(user);

        log.info("User {} upgraded from CLIENT to BARBER", user.getId());

        return getCompleteProfile(email);
    }

    private boolean isProfileComplete(ApplicationUser user) {
        // Sous-comptes de gestion (manager / réceptionniste) : opérationnels d'emblée,
        // pas de profil métier ni de téléphone requis.
        if (user.getUserType() == ApplicationUserType.MANAGER
                || user.getUserType() == ApplicationUserType.RECEPTIONIST
                || user.getUserType() == ApplicationUserType.EMPLOYEE) {
            return true;
        }

        // Vérifications communes
        if (user.getPhone() == null || user.getPhone().isBlank())
            return false;

        // Vérifications spécifiques par type
        switch (user.getUserType()) {
            case CLIENT -> {
                return user.getClient() != null;
            }
            case BARBER -> {
                if (user.getBarber() == null)
                    return false;
                Barber barber = user.getBarber();
                return barber.getSalonName() != null &&
                        !barber.getSalonName().equals("À définir") &&
                        barber.getLocation() != null &&
                        !barber.getLocation().equals("À définir");
            }
            case SALON_OWNER -> {
                // Un propriétaire n'a PAS de profil barbier. Son profil est
                // considéré complet dès qu'il a un téléphone ; son salon se
                // crée séparément via « Mon Salon ».
                return true; // (le téléphone est déjà validé plus haut)
            }
            default -> {
                return true;
            }
        }
    }

    private void updateClientProfile(ApplicationUser user, CompleteProfileRequest request) {
        Client client = user.getClient();
        if (client == null) {
            client = Client.builder()
                    .user(user)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();
        }

        if (request.getPreferences() != null) {
            client.setPreferences(request.getPreferences());
        }

        clientRepository.save(client);
    }

    private void updateBarberProfile(ApplicationUser user, CompleteProfileRequest request) {
        Barber barber = user.getBarber();
        if (barber == null) {
            throw new InvalideEntityException("Profil barbier manquant");
        }

        if (request.getSalonName() != null)
            barber.setSalonName(request.getSalonName());
        if (request.getLocation() != null)
            barber.setLocation(request.getLocation());
        if (request.getDescription() != null)
            barber.setDescription(request.getDescription());
        if (request.getSpecialties() != null)
            barber.setSpecialties(request.getSpecialties());
        if (request.getPriceFrom() != null)
            barber.setPriceFrom(request.getPriceFrom());
        if (request.getWorkingHours() != null)
            barber.setWorkingHours(request.getWorkingHours());

        barberRepository.save(barber);
    }
}
