package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.CreateSalonRequest;
import com.salahtech.BarberShop_Apis.Dtos.SalonDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateSalonRequest;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BarberServices;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.EstablishmentType;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.EstablishmentTypeRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.WorkstationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalonServiceImpl implements SalonService {

    private final SalonRepository salonRepository;
    private final BarberRepository barberRepository;
    private final WorkstationRepository workstationRepository;
    private final EstablishmentTypeRepository establishmentTypeRepository;
    private final BarberServices barberServices;
    private final com.salahtech.BarberShop_Apis.repositories.SalonMembershipRepository membershipRepository;
    private final com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository userRepository;
    private final com.salahtech.BarberShop_Apis.repositories.RoleRepository roleRepository;
    private final com.salahtech.BarberShop_Apis.repositories.EmployeeRepository employeeRepository;

    @Override
    public SalonDto create(CreateSalonRequest req, ApplicationUser owner) {
        Salon salon = new Salon();
        salon.setName(req.getName());
        salon.setAddress(req.getAddress());
        salon.setLatitude(req.getLatitude());
        salon.setLongitude(req.getLongitude());
        salon.setPhoneNumber(req.getPhoneNumber());
        salon.setOwner(owner);
        salon.setType(resolveType(req.getEstablishmentTypeId()));

        Salon saved = salonRepository.save(salon);

        // Membership OWNER : autorité multi-tenant du propriétaire sur son salon.
        if (owner != null && !membershipRepository.existsByUser_IdAndSalon_Id(owner.getId(), saved.getId())) {
            membershipRepository.save(com.salahtech.BarberShop_Apis.models.SalonMembership.builder()
                    .user(owner)
                    .salon(saved)
                    .role(com.salahtech.BarberShop_Apis.Enums.SalonRole.OWNER)
                    .build());
        }

        // Barbier indépendant (ancien modèle) : il devient propriétaire de son salon
        // ET reste prestataire réservable dans ce salon.
        if (owner != null && owner.getUserType() == com.salahtech.BarberShop_Apis.Enums.ApplicationUserType.BARBER) {
            promoteIndependentBarber(owner, saved);
        }

        log.info("Salon créé id={} owner={}", saved.getId(), owner != null ? owner.getId() : null);
        return toDto(saved);
    }

    /** BARBER sans salon → SALON_OWNER (rôle + type) + fiche prestataire dans son nouveau salon. */
    private void promoteIndependentBarber(ApplicationUser user, Salon salon) {
        user.setUserType(com.salahtech.BarberShop_Apis.Enums.ApplicationUserType.SALON_OWNER);
        roleRepository.findByName("SALON_OWNER").ifPresent(r -> {
            java.util.Set<com.salahtech.BarberShop_Apis.models.Role> roles =
                    user.getRoles() == null ? new java.util.HashSet<>() : new java.util.HashSet<>(user.getRoles());
            roles.add(r);
            user.setRoles(roles);
        });
        userRepository.save(user);

        if (employeeRepository.findByUser_Id(user.getId()).isEmpty()) {
            com.salahtech.BarberShop_Apis.models.Barber barber = user.getBarber();
            if (barber != null && barber.getSalon() == null) {
                barber.setSalon(salon);
                barberRepository.save(barber);
            }
            com.salahtech.BarberShop_Apis.models.Employee e = com.salahtech.BarberShop_Apis.models.Employee.builder()
                    .user(user)
                    .barber(barber)
                    .salon(salon)
                    .primarySalon(salon)
                    .salons(new java.util.HashSet<>(java.util.Set.of(salon)))
                    .firstName(user.getFirstName() != null ? user.getFirstName() : "Prestataire")
                    .lastName(user.getLastName() != null ? user.getLastName() : "-")
                    .email(user.getEmail())
                    .role(com.salahtech.BarberShop_Apis.Enums.EmployeeRole.BARBER)
                    .build();
            employeeRepository.save(e);
        }
        log.info("Barbier indépendant {} promu propriétaire du salon {}", user.getId(), salon.getId());
    }

    @Override
    public SalonDto update(Long salonId, UpdateSalonRequest req) {
        Salon salon = getSalonOrThrow(salonId);

        if (req.getName() != null) salon.setName(req.getName());
        if (req.getAddress() != null) salon.setAddress(req.getAddress());
        if (req.getLatitude() != null) salon.setLatitude(req.getLatitude());
        if (req.getLongitude() != null) salon.setLongitude(req.getLongitude());
        if (req.getPhoneNumber() != null) salon.setPhoneNumber(req.getPhoneNumber());
        if (req.getEstablishmentTypeId() != null) salon.setType(resolveType(req.getEstablishmentTypeId()));

        return toDto(salonRepository.save(salon));
    }

    @Override
    public SalonDto setLogo(Long salonId, String url) {
        Salon salon = getSalonOrThrow(salonId);
        salon.setLogoUrl(url);
        return toDto(salonRepository.save(salon));
    }

    @Override
    public SalonDto setCover(Long salonId, String url) {
        Salon salon = getSalonOrThrow(salonId);
        salon.setCoverUrl(url);
        return toDto(salonRepository.save(salon));
    }

    @Override
    @Transactional(readOnly = true)
    public SalonDto findById(Long salonId) {
        return toDto(getSalonOrThrow(salonId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonDto> findMine(ApplicationUser owner) {
        // Salons où l'utilisateur a un membership (owner / manager / réceptionniste),
        // + compat : salons possédés sans membership encore backfillé.
        java.util.LinkedHashMap<Long, Salon> byId = new java.util.LinkedHashMap<>();
        membershipRepository.findByUser_Id(owner.getId())
                .forEach(m -> byId.putIfAbsent(m.getSalon().getId(), m.getSalon()));
        salonRepository.findByOwner_Id(owner.getId())
                .forEach(s -> byId.putIfAbsent(s.getId(), s));
        return byId.values().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalonDto> findAll() {
        return salonRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long salonId) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Salon introuvable: " + salonId);
        }
        // Détache les barbiers pour ne pas violer la FK
        for (Barber b : barberRepository.findBySalon_Id(salonId)) {
            b.setSalon(null);
            barberRepository.save(b);
        }
        salonRepository.deleteById(salonId);
        log.info("Salon supprimé id={}", salonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BarberDto> findBarbers(Long salonId) {
        return barberRepository.findBySalon_Id(salonId).stream()
                .map(b -> barberServices.findById(b.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public void attachBarber(Long salonId, Long barberId) {
        Salon salon = getSalonOrThrow(salonId);
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable: " + barberId));
        barber.setSalon(salon);
        if (salon.getName() != null) barber.setSalonName(salon.getName());
        barberRepository.save(barber);
        log.info("Barbier {} rattaché au salon {}", barberId, salonId);
    }

    @Override
    public void detachBarber(Long salonId, Long barberId) {
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable: " + barberId));
        if (barber.getSalon() != null && barber.getSalon().getId().equals(salonId)) {
            barber.setSalon(null);
            barberRepository.save(barber);
            log.info("Barbier {} détaché du salon {}", barberId, salonId);
        }
    }

    // ---------- helpers ----------

    private Salon getSalonOrThrow(Long salonId) {
        return salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
    }

    private EstablishmentType resolveType(Long typeId) {
        if (typeId == null) return null;
        return establishmentTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Type d'établissement introuvable: " + typeId));
    }

    private SalonDto toDto(Salon s) {
        ApplicationUser owner = s.getOwner();
        EstablishmentType type = s.getType();
        return SalonDto.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .phoneNumber(s.getPhoneNumber())
                .logoUrl(s.getLogoUrl())
                .coverUrl(s.getCoverUrl())
                .ownerId(owner != null ? owner.getId() : null)
                .ownerName(owner != null ? buildName(owner) : null)
                .establishmentTypeId(type != null ? type.getId() : null)
                .establishmentTypeName(type != null ? type.getName() : null)
                .barbersCount(barberRepository.countBySalon_Id(s.getId()))
                .workstationsCount(workstationRepository.countBySalon_Id(s.getId()))
                .build();
    }

    private String buildName(ApplicationUser u) {
        String first = u.getFirstName() != null ? u.getFirstName() : "";
        String last = u.getLastName() != null ? u.getLastName() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? u.getEmail() : full;
    }
}
