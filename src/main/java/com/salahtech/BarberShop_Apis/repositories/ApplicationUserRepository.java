package com.salahtech.BarberShop_Apis.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
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

    @Query("SELECT COUNT(DISTINCT u) FROM ApplicationUser u JOIN u.roles r WHERE r.name = :roleName")
    long countByRolesContaining(@Param("roleName") String roleName);

    long countByAccountNonLockedFalse();

    long countByIsVerifiedFalse();

    long countByIsEnabledTrue();

    long countByIsEnabledFalse();

    /**
     * Recherche d'utilisateurs contactables (messagerie). Exclut soi-même et les
     * comptes désactivés. Si {@code excludeType} est non null, exclut ce type
     * (ex. un client ne contacte que le staff → excludeType = CLIENT).
     */
    @Query("""
            SELECT u FROM ApplicationUser u
            WHERE u.id <> :selfId AND u.isEnabled = true
              AND ( :q = '' OR lower(u.firstName) LIKE lower(concat('%', :q, '%'))
                    OR lower(u.lastName)  LIKE lower(concat('%', :q, '%'))
                    OR lower(u.email)     LIKE lower(concat('%', :q, '%')) )
              AND ( :excludeType IS NULL OR u.userType <> :excludeType )
            ORDER BY u.firstName, u.lastName
            """)
    List<ApplicationUser> searchContacts(@Param("selfId") Long selfId,
                                         @Param("q") String q,
                                         @Param("excludeType") ApplicationUserType excludeType,
                                         Pageable pageable);
}
