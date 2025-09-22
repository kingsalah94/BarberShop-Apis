package com.salahtech.BarberShop_Apis.Validators;

import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientRepository;

@Component
public class UserProfileValidator {

    private ClientRepository clientRepository;
    private BarberRepository barberRepository;

    public void validateUserProfileCreation(ApplicationUser user) {
        // Vérifier qu'un user n'a qu'un seul profil métier
        boolean hasClient = clientRepository.existsByUser_Id(user.getId());
        boolean hasBarber = barberRepository.existsByUser_Id(user.getId());

        if (hasClient && hasBarber) {
            throw new InvalideEntityException(
                    "Un utilisateur ne peut avoir qu'un seul type de profil");
        }

        // Vérifier la cohérence avec le userType
        switch (user.getUserType()) {
            case CLIENT -> {
                if (hasBarber) {
                    throw new InvalideEntityException(
                            "Un CLIENT ne peut pas avoir un profil BARBER");
                }
            }
            case BARBER, SALON_OWNER -> {
                if (hasClient) {
                    throw new InvalideEntityException(
                            "Un BARBER ne peut pas avoir un profil CLIENT");
                }
            }
        }
    }
}
