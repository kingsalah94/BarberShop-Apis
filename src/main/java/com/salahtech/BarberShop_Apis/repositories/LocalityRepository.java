package com.salahtech.BarberShop_Apis.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.salahtech.BarberShop_Apis.models.Locality;

public interface LocalityRepository extends JpaRepository<Locality, Long> {

    List<Locality> findByParentIsNullOrderByNameAsc();

    List<Locality> findByParent_IdOrderByNameAsc(Long parentId);

    boolean existsByParent_Id(Long parentId);

    @Query("select l from Locality l where ((:parentId is null and l.parent is null) or l.parent.id = :parentId) and lower(l.name) = lower(:name)")
    Optional<Locality> findSibling(Long parentId, String name);

    /** Arbre complet (petit) : id, parentId — pour les remontées de statistiques et les descendants. */
    @Query("select l.id, l.parent.id from Locality l")
    List<Object[]> idParentPairs();

    // ── Agrégats par localité (rattachement direct ; la remontée vers les parents se fait en service) ──
    @Query("select s.locality.id, count(s) from Salon s where s.locality is not null group by s.locality.id")
    List<Object[]> salonsPerLocality();

    @Query("select u.locality.id, count(u) from ApplicationUser u where u.locality is not null group by u.locality.id")
    List<Object[]> usersPerLocality();

    @Query("select u.locality.id, count(u) from ApplicationUser u where u.locality is not null and u.userType = com.salahtech.BarberShop_Apis.Enums.ApplicationUserType.CLIENT group by u.locality.id")
    List<Object[]> clientsPerLocality();

    @Query("select b.salon.locality.id, count(b), sum(case when b.startAt >= :since then 1 else 0 end) from SalonBooking b where b.salon.locality is not null group by b.salon.locality.id")
    List<Object[]> bookingsPerLocality(java.time.LocalDateTime since);

    @Query("select s.id from Salon s where s.locality.id in :ids")
    List<Long> salonIdsIn(java.util.Collection<Long> ids);
}
