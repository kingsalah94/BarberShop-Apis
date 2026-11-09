package com.salahtech.BarberShop_Apis.Services;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserProfileMigration {

    private final ApplicationUserRepository userRepository;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void migrateExistingUsers() {
        log.info("Checking for users without business profiles...");

        // Clients sans profil
        List<ApplicationUser> clientUsersWithoutProfile = userRepository
                .findByUserTypeAndClientIsNull(ApplicationUserType.CLIENT);

        for (ApplicationUser user : clientUsersWithoutProfile) {
            Client client = Client.builder()
                    .user(user)
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .email(user.getEmail())
                    .phone(user.getPhone())
                    .build();
            clientRepository.save(client);
            log.info("Created client profile for user {}", user.getId());
        }

        // Barbers sans profil
        List<ApplicationUser> barberUsersWithoutProfile = userRepository
                .findByUserTypeAndBarberIsNull(ApplicationUserType.BARBER);

        for (ApplicationUser user : barberUsersWithoutProfile) {
            Barber barber = Barber.builder()
                    .user(user)
                    .name(user.getFullName())
                    .salonName("À compléter")
                    .location("À compléter")
                    .priceFrom(BigDecimal.ZERO)
                    .available(false)
                    .build();
            barberRepository.save(barber);
            log.info("Created barber profile for user {}", user.getId());
        }
    }
}
