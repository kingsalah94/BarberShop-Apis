package com.salahtech.BarberShop_Apis.reppsitories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@Repository
public interface ApplicationUserRepository
                extends JpaRepository<ApplicationUser, Long>, JpaSpecificationExecutor<ApplicationUser> {

        Optional<ApplicationUser> findByEmail(String email);

        boolean existsByEmail(String email);

        /** ✅ chercher par nom de rôle (ex: "ADMIN") */
        List<ApplicationUser> findByRoles_NameIgnoreCase(String roleName);

        @Query("select u from ApplicationUser u where lower(u.firstName) like lower(concat('%', :name,'%'))")
        List<ApplicationUser> findByFirstNameContaining(@Param("name") String name);

        @Query("select u from ApplicationUser u where u.isEnabled = true")
        List<ApplicationUser> findActiveUsers();

        Optional<ApplicationUser> findByVerificationToken(String token);

        Optional<ApplicationUser> findByResetPasswordToken(String token);

        /** ✅ provider est un Enum dans l’entité */
        @Query("SELECT u FROM ApplicationUser u WHERE u.provider = :provider AND u.providerId = :providerId")
        Optional<ApplicationUser> findByProviderAndProviderId(@Param("provider") AuthProvider provider,
                        @Param("providerId") String providerId);

        boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);

        boolean existsByPhone(String phone);

        Optional<ApplicationUser> findByPhone(String phone);

        // List<ApplicationUser> findByUserTypeAndClientIsNull(ApplicationUserType
        // client);

        // List<ApplicationUser> findByUserTypeAndBarberIsNull(ApplicationUserType
        // barber);

        // Dans ApplicationUserRepository
        @Query("SELECT u FROM ApplicationUser u WHERE u.userType = :type AND u.client IS NULL")
        List<ApplicationUser> findByUserTypeAndClientIsNull(@Param("type") ApplicationUserType type);

        @Query("SELECT u FROM ApplicationUser u WHERE u.userType = :type AND u.barber IS NULL")
        List<ApplicationUser> findByUserTypeAndBarberIsNull(@Param("type") ApplicationUserType type);

        @Query("""
                        SELECT u FROM ApplicationUser u
                        LEFT JOIN FETCH u.client
                        LEFT JOIN FETCH u.barber
                        WHERE u.email = :email
                        """)
        Optional<ApplicationUser> findByEmailWithProfiles(@Param("email") String email);

        // Stats
        @Query("""
                        SELECT
                            COUNT(u) as total,
                            COUNT(CASE WHEN u.userType = 'CLIENT' THEN 1 END) as clients,
                            COUNT(CASE WHEN u.userType = 'BARBER' THEN 1 END) as barbers,
                            COUNT(CASE WHEN u.client IS NOT NULL THEN 1 END) as withClientProfile,
                            COUNT(CASE WHEN u.barber IS NOT NULL THEN 1 END) as withBarberProfile
                        FROM ApplicationUser u
                        """)
        List<Object[]> getUserProfileStatistics();
}
