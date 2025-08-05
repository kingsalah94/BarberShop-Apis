package com.salahtech.BarberShop_Apis.reppsitories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@Repository
public interface ApplicationUserRepository extends JpaRepository<ApplicationUser, Long> {
    
    Optional<ApplicationUser> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    List<ApplicationUser> findByRole(AuthProvider role);
    
    @Query("SELECT u FROM User u WHERE u.name LIKE %:name%")
    List<ApplicationUser> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true")
    List<ApplicationUser> findActiveUsers();

    Optional<ApplicationUser> findByVerificationToken(String token);
    
    Optional<ApplicationUser> findByResetPasswordToken(String token);
    
    @Query("SELECT u FROM User u WHERE u.provider = ?1 AND u.providerId = ?2")
    Optional<ApplicationUser> findByProviderAndProviderId(String provider, String providerId);
}
