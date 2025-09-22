package com.salahtech.BarberShop_Apis.Services;

import java.math.BigDecimal;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest.BarberProfileData;
import com.salahtech.BarberShop_Apis.Dtos.CreateUserWithProfileRequest.ClientProfileData;
import com.salahtech.BarberShop_Apis.Dtos.UserProfileDto;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

@Service
@Transactional
public class UserManagementService {

    private ApplicationUserService userService;
    private RoleRepository roleRepository;
    private ClientRepository clientRepository;
    private BarberRepository barberRepository;
    private ApplicationUserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @Transactional
    public UserProfileDto createUserWithProfile(CreateUserWithProfileRequest request) {
        // 1. Créer l'ApplicationUser
        ApplicationUser user = new ApplicationUser();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setUserType(request.getUserType());

        // Assigner le rôle approprié
        Role role = roleRepository.findByName(request.getUserType().name())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        user.setRoles(Set.of(role));

        user = userRepository.save(user);

        // 2. Créer le profil métier selon le type
        ClientDto clientDto = null;
        BarberDto barberDto = null;

        switch (request.getUserType()) {
            case CLIENT -> {
                Client client = createClientFromRequest(user, request.getClientData());
                clientDto = ClientDto.fromEntity(client);
            }
            case BARBER, SALON_OWNER -> {
                Barber barber = createBarberFromRequest(user, request.getBarberData());
                barberDto = BarberDto.fromEntity(barber);
            }
        }

        // 3. Retourner le profil complet
        return UserProfileDto.builder()
                .user(ApplicationUserDto.fromEntity(user))
                .client(clientDto)
                .barber(barberDto)
                .build();
    }

    private Client createClientFromRequest(ApplicationUser user, ClientProfileData data) {
        Client client = Client.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .preferences(data != null ? data.getPreferences() : null)
                .notes(data != null ? data.getNotes() : null)
                .build();

        return clientRepository.save(client);
    }

    private Barber createBarberFromRequest(ApplicationUser user, BarberProfileData data) {
        if (data == null) {
            throw new InvalideEntityException("Les données du barbier sont obligatoires");
        }

        Barber barber = Barber.builder()
                .user(user)
                .name(user.getFullName())
                .salonName(data.getSalonName())
                .location(data.getLocation())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .description(data.getDescription())
                .specialties(data.getSpecialties())
                .priceFrom(data.getPriceFrom())
                .workingHours(data.getWorkingHours())
                .available(true)
                .rating(BigDecimal.ZERO)
                .reviewsCount(0)
                .build();

        return barberRepository.save(barber);
    }
}
