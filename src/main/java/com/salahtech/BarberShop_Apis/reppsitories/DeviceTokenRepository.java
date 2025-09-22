package com.salahtech.BarberShop_Apis.reppsitories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.salahtech.BarberShop_Apis.models.DeviceToken;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long>,
              JpaSpecificationExecutor<DeviceToken> {

       /** ✅ basiques */
       Optional<DeviceToken> findByToken(String token);

       boolean existsByToken(String token);

       /** ✅ via relation */
       Optional<DeviceToken> findByUser_Id(Long userId);

       Optional<DeviceToken> findByUser_IdAndActive(Long userId, Boolean active);

       List<DeviceToken> findByUser_IdIn(List<Long> userIds);

       List<DeviceToken> findByUser_IdInAndActive(List<Long> userIds, Boolean active);

       List<DeviceToken> findByActive(Boolean active);

       List<DeviceToken> findByDeviceType(String deviceType);

       List<DeviceToken> findByDeviceTypeAndActive(String deviceType, Boolean active);

       void deleteByUser_Id(Long userId);

       void deleteByActive(Boolean active);

       /** ✅ stats/maintenance */
       @Query("SELECT COUNT(dt) FROM DeviceToken dt WHERE dt.active = :active")
       Long countByActive(@Param("active") Boolean active);

       @Query("SELECT COUNT(dt) FROM DeviceToken dt WHERE dt.deviceType = :deviceType AND dt.active = :active")
       Long countByDeviceTypeAndActive(@Param("deviceType") String deviceType,
                     @Param("active") Boolean active);

       /** tokens « expirés » au sens d’inactivité (ex: last update trop ancien) */
       @Query("SELECT dt FROM DeviceToken dt WHERE dt.active = true AND dt.updatedAt < :expiredDate")
       List<DeviceToken> findExpiredTokens(@Param("expiredDate") LocalDateTime expiredDate);

       /** actifs + user.enabled */
       @Query("""
                     SELECT dt FROM DeviceToken dt
                     WHERE dt.active = true
                       AND dt.user.isEnabled = true
                     """)
       List<DeviceToken> findActiveTokensForEnabledUsers();

       @Query("SELECT dt.deviceType, COUNT(dt) FROM DeviceToken dt WHERE dt.active = :active GROUP BY dt.deviceType")
       List<Object[]> getTokenStatsByDeviceType(@Param("active") Boolean active);
}
