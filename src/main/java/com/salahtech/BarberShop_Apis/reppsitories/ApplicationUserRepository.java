package com.salahtech.BarberShop_Apis.reppsitories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

@Repository
public interface ApplicationUserRepository
        extends JpaRepository<ApplicationUser, Long>, JpaSpecificationExecutor<ApplicationUser> {

    Optional<ApplicationUser> findByEmail(String email);

    boolean existsByEmail(String email);

    List<ApplicationUser> findByRoles(AuthProvider roles);

    // ✅ Corrige l’ancienne méthode erronée findByRoles(AuthProvider roles)
    List<ApplicationUser> findByRoles_NameIgnoreCase(String roleName);

    @Query("select u from ApplicationUser u where lower(u.firstName) like lower(concat('%', :name,'%'))")
    List<ApplicationUser> findByFirstNameContaining(String name);

    @Query("select u from ApplicationUser u where u.isEnabled = true")
    List<ApplicationUser> findActiveUsers();

    Optional<ApplicationUser> findByVerificationToken(String token);

    Optional<ApplicationUser> findByResetPasswordToken(String token);

    // // ✅ HQL corrige : utiliser le nom de l'entité, pas celui de la table
    // @Query("SELECT u FROM ApplicationUser u WHERE u.firstName LIKE %:name%")
    // List<ApplicationUser> findByFirstNameContaining(@Param("name") String name);

    // @Query(
    // value = "SELECT * FROM application_users WHERE CONCAT(first_name, ' ',
    // last_name) ILIKE %:fullName%",
    // nativeQuery = true
    // )
    // List<ApplicationUser> searchByFullName(@Param("fullName") String fullName);

    // @Query("SELECT u FROM ApplicationUser u WHERE u.isEnabled = true")
    // List<ApplicationUser> findActiveUsers();

    // Optional<ApplicationUser> findByVerificationToken(String token);

    // Optional<ApplicationUser> findByResetPasswordToken(String token);

    @Query("SELECT u FROM ApplicationUser u WHERE u.provider = :provider AND u.providerId = :providerId")
    Optional<ApplicationUser> findByProviderAndProviderId(@Param("provider") String provider,
            @Param("providerId") String providerId);
}
