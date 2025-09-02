package com.salahtech.BarberShop_Apis.reppsitories;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.salahtech.BarberShop_Apis.models.DeviceToken;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long>,
              JpaSpecificationExecutor<DeviceToken> {

       // /**
       // * Trouver un token par userId
       // */
       // Optional<DeviceToken> findByUserId(Long userId);

       // /**
       // * Trouver un token par userId et statut actif
       // */
       // Optional<DeviceToken> findByUserIdAndActive(Long userId, Boolean active);

       // /**
       // * Trouver tous les tokens par statut actif
       // */
       // List<DeviceToken> findByActive(Boolean active);

       // /**
       // * Trouver tous les tokens par type d'appareil et statut actif
       // */
       // List<DeviceToken> findByDeviceTypeAndActive(String deviceType, Boolean
       // active);

       // /**
       // * Trouver un token par la valeur du token lui-même
       // */
       // Optional<DeviceToken> findByToken(String token);

       // /**
       // * Trouver tous les tokens d'un type d'appareil spécifique
       // */
       // List<DeviceToken> findByDeviceType(String deviceType);

       /**
        * Trouver tous les tokens d'une liste d'utilisateurs
        */
       // List<DeviceToken> findByUserIdIn(List<Long> userIds);

       // /**
       // * Trouver tous les tokens actifs d'une liste d'utilisateurs
       // */
       // List<DeviceToken> findByUserIdInAndActive(List<Long> userIds, Boolean
       // active);

       /**
        * Compter le nombre de tokens actifs
        */
       @Query("SELECT COUNT(dt) FROM DeviceToken dt WHERE dt.active = :active")
       Long countByActive(@Param("active") Boolean active);

       /**
        * Compter le nombre de tokens actifs par type d'appareil
        */
       @Query("SELECT COUNT(dt) FROM DeviceToken dt WHERE dt.deviceType = :deviceType AND dt.active = :active")
       Long countByDeviceTypeAndActive(@Param("deviceType") String deviceType, @Param("active") Boolean active);

       // /**
       // * Vérifier si un token existe déjà
       // */
       // Boolean existsByToken(String token);

       // /**
       // * Vérifier si un utilisateur a un token actif
       // */
       // Boolean existsByUserIdAndActive(Long userId, Boolean active);

       // /**
       // * Supprimer tous les tokens d'un utilisateur
       // */
       // void deleteByUserId(Long userId);

       // /**
       // * Supprimer tous les tokens inactifs
       // */
       // void deleteByActive(Boolean active);

       // /**
       // * Requête personnalisée pour obtenir les tokens avec informations utilisateur
       // */
       // @Query("SELECT dt FROM DeviceToken dt WHERE dt.active = true AND dt.userId IN
       // " +
       // "(SELECT u.id FROM ApplicationUser u WHERE u.isEnabled = true)")
       // List<DeviceToken> findActiveTokensForEnabledUsers();

       /**
        * Requête pour obtenir les tokens expirés (si vous avez un champ createdAt ou
        * updatedAt)
        */
       @Query("SELECT dt FROM DeviceToken dt WHERE dt.active = true AND " +
                     "dt.updatedAt < :expiredDate")
       List<DeviceToken> findExpiredTokens(@Param("expiredDate") java.time.LocalDateTime expiredDate);

       // /**
       // * Requête pour obtenir les statistiques des tokens par type d'appareil
       // */
       // @Query("SELECT dt.deviceType, COUNT(dt) FROM DeviceToken dt WHERE dt.active =
       // :active " +
       // "GROUP BY dt.deviceType")
       // List<Object[]> getTokenStatsByDeviceType(@Param("active") Boolean active);

       Optional<DeviceToken> findByUserId(Long userId);

       Optional<DeviceToken> findByUserIdAndActive(Long userId, Boolean active);

       List<DeviceToken> findByActive(Boolean active);

       List<DeviceToken> findByDeviceTypeAndActive(String deviceType, Boolean active);

       Optional<DeviceToken> findByToken(String token);

       List<DeviceToken> findByDeviceType(String deviceType);

       List<DeviceToken> findByUserIdIn(List<Long> userIds);

       List<DeviceToken> findByUserIdInAndActive(List<Long> userIds, Boolean active);

       boolean existsByToken(String token);

       boolean existsByUserIdAndActive(Long userId, Boolean active);

       void deleteByUserId(Long userId);

       void deleteByActive(Boolean active);

       @Query("select dt from DeviceToken dt where dt.active = true and dt.userId in " +
                     "(select u.id from ApplicationUser u where u.isEnabled = true)")
       List<DeviceToken> findActiveTokensForEnabledUsers();

       @Query("select dt from DeviceToken dt where dt.active = true and dt.updatedAt < :expiredDate")
       List<DeviceToken> findExpiredTokens(@Param("expiredDate") Instant expiredDate);

       @Query("select dt.deviceType, count(dt) from DeviceToken dt where dt.active = :active group by dt.deviceType")
       List<Object[]> getTokenStatsByDeviceType(@Param("active") Boolean active);
}
