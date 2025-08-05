package com.salahtech.BarberShop_Apis.Services;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

public class ApplicationUserService {

     @Autowired
    private ApplicationUserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    public ApplicationUser findByEmailOrCreate(String email, String name, AuthProvider provider, String providerId) {
        Optional<ApplicationUser> existingUser = userRepository.findByEmail(email);
        
        if (existingUser.isPresent()) {
            ApplicationUser user = existingUser.get();
            user.setLastLogin(LocalDateTime.now());
            return userRepository.save(user);
        }
        
        // Créer un nouvel utilisateur OAuth2
        ApplicationUser newUser = new ApplicationUser();
        newUser.setEmail(email);
        
        // Diviser le nom complet
        String[] nameParts = name.split(" ", 2);
        newUser.setFirstName(nameParts[0]);
        if (nameParts.length > 1) {
            newUser.setLastName(nameParts[1]);
        } else {
            newUser.setLastName("");
        }
        
        newUser.setProvider(provider);
        newUser.setProviderId(providerId);
        newUser.setIsEnabled(true);
        newUser.setIsVerified(true); // Les comptes OAuth2 sont automatiquement vérifiés
        newUser.setPassword(""); // Pas de mot de passe pour OAuth2
        
        // Assigner le rôle CLIENT par défaut
        Role clientRole = roleRepository.findByName("CLIENT")
                .orElseThrow(() -> new ResourceNotFoundException("Rôle CLIENT non trouvé"));
        newUser.setRoles(Set.of(clientRole));
        
        return userRepository.save(newUser);
    }
    
    public ApplicationUser findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'ID : " + id));
    }
    
    public ApplicationUser findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));
    }
}
