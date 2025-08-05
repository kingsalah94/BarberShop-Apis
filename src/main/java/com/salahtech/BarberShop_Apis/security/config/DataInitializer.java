package com.salahtech.BarberShop_Apis.security.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.PermissionRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializePermissions();
        initializeRoles();
    }
    
    private void initializePermissions() {
        createPermissionIfNotExists("USER_READ", "Lire les informations utilisateur");
        createPermissionIfNotExists("USER_WRITE", "Modifier les informations utilisateur");
        createPermissionIfNotExists("USER_DELETE", "Supprimer un utilisateur");
        
        createPermissionIfNotExists("CLIENT_ACCESS", "Accès client");
        createPermissionIfNotExists("BOOKING_CREATE", "Créer une réservation");
        createPermissionIfNotExists("BOOKING_READ", "Voir ses réservations");
        createPermissionIfNotExists("BOOKING_UPDATE", "Modifier ses réservations");
        createPermissionIfNotExists("BOOKING_DELETE", "Annuler ses réservations");
        
        createPermissionIfNotExists("BARBER_ACCESS", "Accès barbier");
        createPermissionIfNotExists("BARBER_BOOKING_MANAGE", "Gérer les réservations barbier");
        createPermissionIfNotExists("BARBER_SCHEDULE_MANAGE", "Gérer les horaires");
        createPermissionIfNotExists("BARBER_PROFILE_MANAGE", "Gérer le profil barbier");
        
        createPermissionIfNotExists("SALON_OWNER_ACCESS", "Accès propriétaire salon");
        createPermissionIfNotExists("SALON_MANAGE", "Gérer le salon");
        createPermissionIfNotExists("SALON_STAFF_MANAGE", "Gérer l'équipe");
        createPermissionIfNotExists("SALON_ANALYTICS", "Voir les statistiques salon");
        
        createPermissionIfNotExists("ADMIN_ACCESS", "Accès administrateur");
        createPermissionIfNotExists("ADMIN_USER_MANAGE", "Gérer tous les utilisateurs");
        createPermissionIfNotExists("ADMIN_SYSTEM_MANAGE", "Gérer le système");
    }
    
    private void initializeRoles() {
        // Rôle CLIENT
        Role clientRole = createRoleIfNotExists("CLIENT", "Client de l'application");
        if (clientRole != null) {
            Set<Permission> clientPermissions = Set.of(
                permissionRepository.findByName("USER_READ").orElseThrow(),
                permissionRepository.findByName("USER_WRITE").orElseThrow(),
                permissionRepository.findByName("CLIENT_ACCESS").orElseThrow(),
                permissionRepository.findByName("BOOKING_CREATE").orElseThrow(),
                permissionRepository.findByName("BOOKING_READ").orElseThrow(),
                permissionRepository.findByName("BOOKING_UPDATE").orElseThrow(),
                permissionRepository.findByName("BOOKING_DELETE").orElseThrow()
            );
            clientRole.setPermissions(clientPermissions);
            roleRepository.save(clientRole);
        }
        
        // Rôle BARBER
        Role barberRole = createRoleIfNotExists("BARBER", "Barbier/Coiffeur");
        if (barberRole != null) {
            Set<Permission> barberPermissions = Set.of(
                permissionRepository.findByName("USER_READ").orElseThrow(),
                permissionRepository.findByName("USER_WRITE").orElseThrow(),
                permissionRepository.findByName("BARBER_ACCESS").orElseThrow(),
                permissionRepository.findByName("BARBER_BOOKING_MANAGE").orElseThrow(),
                permissionRepository.findByName("BARBER_SCHEDULE_MANAGE").orElseThrow(),
                permissionRepository.findByName("BARBER_PROFILE_MANAGE").orElseThrow()
            );
            barberRole.setPermissions(barberPermissions);
            roleRepository.save(barberRole);
        }
        
        // Rôle SALON_OWNER
        Role salonOwnerRole = createRoleIfNotExists("SALON_OWNER", "Propriétaire de salon");
        if (salonOwnerRole != null) {
            Set<Permission> salonOwnerPermissions = Set.of(
                permissionRepository.findByName("USER_READ").orElseThrow(),
                permissionRepository.findByName("USER_WRITE").orElseThrow(),
                permissionRepository.findByName("SALON_OWNER_ACCESS").orElseThrow(),
                permissionRepository.findByName("SALON_MANAGE").orElseThrow(),
                permissionRepository.findByName("SALON_STAFF_MANAGE").orElseThrow(),
                permissionRepository.findByName("SALON_ANALYTICS").orElseThrow(),
                permissionRepository.findByName("BARBER_BOOKING_MANAGE").orElseThrow()
            );
            salonOwnerRole.setPermissions(salonOwnerPermissions);
            roleRepository.save(salonOwnerRole);
        }
        
        // Rôle ADMIN
        Role adminRole = createRoleIfNotExists("ADMIN", "Administrateur système");
        if (adminRole != null) {
            Set<Permission> adminPermissions = Set.of(
                permissionRepository.findByName("ADMIN_ACCESS").orElseThrow(),
                permissionRepository.findByName("ADMIN_USER_MANAGE").orElseThrow(),
                permissionRepository.findByName("ADMIN_SYSTEM_MANAGE").orElseThrow(),
                permissionRepository.findByName("USER_READ").orElseThrow(),
                permissionRepository.findByName("USER_WRITE").orElseThrow(),
                permissionRepository.findByName("USER_DELETE").orElseThrow()
            );
            adminRole.setPermissions(adminPermissions);
            roleRepository.save(adminRole);
        }
    }
    
    private Permission createPermissionIfNotExists(String name, String description) {
        if (!permissionRepository.existsByName(name)) {
            Permission permission = new Permission(name, description);
            return permissionRepository.save(permission);
        }
        return null;
    }
    
    private Role createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role(name, description);
            return roleRepository.save(role);
        }
        return roleRepository.findByName(name).orElse(null);
    }
}
