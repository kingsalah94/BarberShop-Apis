package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.AOP.LogActivity;
import com.salahtech.BarberShop_Apis.Dtos.BarberClientSummaryDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Dtos.ClientProfileDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateClientDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientDashboardStatsDto;
import com.salahtech.BarberShop_Apis.models.ClientFavorite;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ClientService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.SalonBooking;
import com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonReviewRepository;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientFavoriteRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientRepository;
import com.salahtech.BarberShop_Apis.repositories.ReviewRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ApplicationUserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ClientFavoriteRepository clientFavoriteRepository;
    private final BarberRepository barberRepository;
    private final ReviewRepository reviewRepository;
    private final SalonBookingRepository salonBookingRepository;
    private final SalonReviewRepository salonReviewRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    @LogActivity(action = "CREATE_CLIENT", entityName = "Client")
    public ClientDto save(ClientDto dto) {
        log.info("Saving client: {}", dto.getEmail());

        // Validation
        if (dto.getUserId() == null) {
            throw new InvalideEntityException("L'ID utilisateur est obligatoire",
                    com.salahtech.BarberShop_Apis.Enums.ErrorCodes.INVALID_ENTITY);
        }

        // Vérifier l'unicité
        if (dto.getId() == null) { // Création
            if (clientRepository.existsByUserId(dto.getUserId())) {
                throw new ConflictException("Un profil client existe déjà pour cet utilisateur");
            }
            if (dto.getEmail() != null && clientRepository.existsByEmail(dto.getEmail())) {
                throw new ConflictException("Cet email est déjà utilisé");
            }
            if (dto.getPhone() != null && clientRepository.existsByPhone(dto.getPhone())) {
                throw new ConflictException("Ce numéro de téléphone est déjà utilisé");
            }
        }
        // TODO doi creer le compte du client au lieu de chercher le user
        // ApplicationUser userAccount = userServiceImpl.register(RegisterRequestDTO
        // req)
        // Charger l'utilisateur
        ApplicationUser user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        Client client = dto.getId() != null ? clientRepository.findById(dto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé")) : new Client();

        // Mapping
        client.setUser(user);
        client.setFirstName(dto.getFirstName() != null ? dto.getFirstName() : user.getFirstName());
        client.setLastName(dto.getLastName() != null ? dto.getLastName() : user.getLastName());
        client.setEmail(dto.getEmail() != null ? dto.getEmail() : user.getEmail());
        client.setPhone(dto.getPhone() != null ? dto.getPhone() : user.getPhone());
        client.setPreferences(dto.getPreferences());
        client.setNotes(dto.getNotes());

        client = clientRepository.save(client);
        log.info("Client saved with id: {}", client.getId());

        return ClientDto.fromEntity(client);
    }

    // ==========================================
    // 🟢 CRÉATION
    // ==========================================
    @Override
    @LogActivity(action = "CREATE_CLIENT_WITH_FILE", entityName = "Client")
    public ClientDto create(ClientDto dto, MultipartFile file) {
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé.");
        }

        // 2. Créer l'User (Authentification)
        ApplicationUser user = new ApplicationUser();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        // Mot de passe (soit fourni, soit par défaut)
        String rawPassword = (dto.getPassword() != null && !dto.getPassword().isEmpty())
                ? dto.getPassword()
                : "Client123!"; // Mot de passe par défaut
        user.setPassword(passwordEncoder.encode(rawPassword));

        ApplicationUserType type = user.getUserType() != null ? user.getUserType() : ApplicationUserType.CLIENT;
        user.setUserType(type);
        user.setRoles(Set.of(getRoleByUserType(type)));
        // Rôles
        // user.setRoles(Collections.singletonList("ROLE_CLIENT")); // Adapte selon ta
        // gestion des rôles (Enum ou String)

        // 3. Gestion de l'Image
        if (file != null && !file.isEmpty()) {
            String imageUrl = fileStorageService.storeFile(file); // Méthode qui sauvegarde et renvoie l'URL
            user.setProfileImageUrl(imageUrl);
        }

        // 4. Créer l'entité Client (Métier)
        Client client = new Client();
        client.setUser(user); // Lien User <-> Client
        client.setNotes(dto.getNotes());
        // client.setActive(true); // Si tu as un champ active

        // Lier dans l'autre sens si nécessaire (Bidirectionnel)
        user.setClient(client);

        // 5. Sauvegarder (Cascade devrait sauver les deux, sinon sauve user puis
        // client)
        userRepository.save(user);
        // clientRepository.save(client); // Normalement géré par le cascade de User

        // 6. Retourner le DTO
        return mapToDto(client);
    }

    private Role getRoleByUserType(ApplicationUserType userType) {
        String roleName = switch (userType) {
            case CLIENT -> "CLIENT";
            case BARBER -> "BARBER";
            case SALON_OWNER -> "SALON_OWNER";
            case ADMIN -> "ADMIN";
            case MANAGER -> "MANAGER";
            case RECEPTIONIST -> "RECEPTIONIST";
            case EMPLOYEE -> "BARBER";
        };
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle " + roleName + " non trouvé en base"));
    }

    // ==========================================
    // 🟠 MISE À JOUR
    // ==========================================
    @Override
    @LogActivity(action = "UPDATE_CLIENT", entityName = "Client")
    public ClientDto update(Long id, ClientDto dto, MultipartFile file) {
        // 1. Récupérer le Client
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Client introuvable"));

        ApplicationUser user = client.getUser();

        // 2. Mettre à jour les infos de base
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());

        // Vérif email unique si changé
        if (!user.getEmail().equals(dto.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé.");
            }
            user.setEmail(dto.getEmail());
        }

        // 3. Gestion de l'Image (Si nouveau fichier fourni)
        if (file != null && !file.isEmpty()) {
            // Optionnel : Supprimer l'ancienne image via fileStorageService.delete(...)
            String imageUrl = fileStorageService.storeFile(file);
            user.setProfileImageUrl(imageUrl);
        }

        // 4. Mettre à jour les infos spécifiques Client
        client.setNotes(dto.getNotes());

        // 5. Sauvegarder
        userRepository.save(user); // Sauve le tout via Cascade

        return mapToDto(client);
    }

    // ==========================================
    // 🛠️ MAPPER (Ou utilise MapStruct)
    // ==========================================
    private ClientDto mapToDto(Client client) {
        ApplicationUser user = client.getUser();
        return ClientDto.builder()
                .id(client.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .notes(client.getNotes())
                .build();
    }

    @Override
    public ClientDto findById(Long id) {
        return clientRepository.findById(id)
                .map(ClientDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'ID: " + id));
    }

    @Override
    public ClientDto findByUserId(Long userId) {
        return clientRepository.findByUserId(userId)
                .map(ClientDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé pour l'utilisateur: " + userId));
    }

    @Override
    public ClientDto findByEmail(String email) {
        return clientRepository.findByEmail(email)
                .map(ClientDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec l'email: " + email));
    }

    @Override
    public ClientDto findByPhone(String phone) {
        return clientRepository.findByPhone(phone)
                .map(ClientDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé avec le téléphone: " + phone));
    }

    @Override
    public List<ClientDto> findAll() {
        return clientRepository.findAll().stream()
                .map(ClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @LogActivity(action = "DELETE_CLIENT", entityName = "Client")
    public void delete(Long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client non trouvé avec l'ID: " + id);
        }
        clientRepository.deleteById(id);
        log.info("Client deleted with id: {}", id);
    }

    @Override
    public Page<ClientDto> search(String query, Boolean hasBookings, LocalDateTime createdFrom,
            LocalDateTime createdTo, Pageable pageable) {
        Specification<Client> spec = (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.isBlank()) {
                String likePattern = "%" + query.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern),
                        criteriaBuilder.like(root.get("phone"), likePattern)));
            }

            if (hasBookings != null) {
                if (hasBookings) {
                    predicates.add(criteriaBuilder.isNotEmpty(root.get("bookings")));
                } else {
                    predicates.add(criteriaBuilder.isEmpty(root.get("bookings")));
                }
            }

            if (createdFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }

            if (createdTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return clientRepository.findAll(spec, pageable).map(ClientDto::fromEntity);
    }

    @Override
    public List<BookingDto> getClientBookings(Long clientId) {
        return bookingRepository.findByClient_Id(clientId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> getClientBookingsByStatus(Long clientId, String status) {
        BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
        return bookingRepository.findByClient_Id(clientId).stream()
                .filter(b -> b.getStatus() == bookingStatus)
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public BookingDto getLastBooking(Long clientId) {
        return bookingRepository.findByClient_Id(clientId).stream()
                .max((b1, b2) -> {
                    LocalDateTime d1 = b1.getStartAt() != null ? b1.getStartAt() : b1.getBookingDate();
                    LocalDateTime d2 = b2.getStartAt() != null ? b2.getStartAt() : b2.getBookingDate();
                    return d1.compareTo(d2);
                })
                .map(BookingDto::fromEntity)
                .orElse(null);
    }

    @Override
    public Integer countClientBookings(Long clientId) {
        return bookingRepository.findByClient_Id(clientId).size();
    }

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_PREFERENCES", entityName = "Client")
    public ClientDto updatePreferences(Long clientId, String preferences) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));
        client.setPreferences(preferences);
        return ClientDto.fromEntity(clientRepository.save(client));
    }

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_NOTES", entityName = "Client")
    public ClientDto updateNotes(Long clientId, String notes) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));
        client.setNotes(notes);
        return ClientDto.fromEntity(clientRepository.save(client));
    }

    @Override
    @Transactional
    @LogActivity(action = "UPDATE_CONTACT", entityName = "Client")
    public ClientDto updateContact(Long clientId, String email, String phone) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        // Vérifier l'unicité
        if (email != null && !email.equals(client.getEmail()) && clientRepository.existsByEmail(email)) {
            throw new ConflictException("Cet email est déjà utilisé");
        }
        if (phone != null && !phone.equals(client.getPhone()) && clientRepository.existsByPhone(phone)) {
            throw new ConflictException("Ce numéro de téléphone est déjà utilisé");
        }

        if (email != null)
            client.setEmail(email);
        if (phone != null)
            client.setPhone(phone);

        return ClientDto.fromEntity(clientRepository.save(client));
    }

    @Override
    public ClientStatsDto getClientStats(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));

        List<Booking> bookings = client.getBookings();

        int totalBookings = bookings.size();
        int completedBookings = (int) bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        int cancelledBookings = (int) bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();

        double totalSpent = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .mapToDouble(b -> b.getTotalPrice() != null ? b.getTotalPrice().doubleValue() : 0)
                .sum();

        LocalDateTime firstBooking = bookings.stream()
                .map(b -> b.getCreatedAt())
                .filter(d -> d != null)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime lastBooking = bookings.stream()
                .map(b -> b.getStartAt() != null ? b.getStartAt() : b.getBookingDate())
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<String> favoriteServices = bookings.stream()
                .filter(b -> b.getBarberService() != null)
                .collect(Collectors.groupingBy(b -> b.getBarberService().getName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new ClientStatsDto(
                clientId, totalBookings, completedBookings, cancelledBookings,
                totalSpent, firstBooking, lastBooking, 0.0, favoriteServices);
    }

    @Override
    public List<ClientDto> getTopClients(int limit) {
        // Récupérer les clients avec le plus de réservations complétées
        return clientRepository.findAll().stream()
                .sorted((c1, c2) -> {
                    long count1 = c1.getBookings().stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
                    long count2 = c2.getBookings().stream()
                            .filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
                    return Long.compare(count2, count1);
                })
                .limit(limit)
                .map(ClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientDto> getInactiveClients(int daysInactive) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysInactive);

        return clientRepository.findAll().stream()
                .filter(client -> {
                    LocalDateTime lastActivity = client.getBookings().stream()
                            .map(b -> b.getStartAt() != null ? b.getStartAt() : b.getBookingDate())
                            .filter(d -> d != null)
                            .max(LocalDateTime::compareTo)
                            .orElse(client.getCreatedAt());
                    return lastActivity.isBefore(threshold);
                })
                .map(ClientDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEmail(String email) {
        return clientRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return clientRepository.existsByPhone(phone);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return clientRepository.existsByUserId(userId);
    }

    /**
     * Get client profile with all information
     */
    @Override
    public ClientProfileDto getClientProfile(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        ApplicationUser user = client.getUser();

        return ClientProfileDto.builder()
                .id(client.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .profilePictureUrl(user.getProfileImageUrl())
                .phoneNumber(client.getPhone())
                .address(client.getAddress())
                .city(client.getCity())
                .postalCode(client.getPostalCode())
                .birthDate(client.getBirthDate())
                .preferences(client.getPreferences())
                .loyaltyPoints(client.getLoyaltyPoints())
                .completedBookingsCount(client.getCompletedBookingsCount())
                .upcomingBookingsCount(client.getUpcomingBookingsCount())
                .favoritesCount(clientFavoriteRepository.countByClientId(clientId).intValue())
                .reviewsCount(reviewRepository.countByClientId(clientId).intValue())
                .memberSince(client.getCreatedAt())
                .lastUpdated(client.getUpdatedAt())
                .build();
    }

    /**
     * Update client profile
     */
    @Override
    @Transactional
    @LogActivity(action = "UPDATE_CLIENT_PROFILE", entityName = "Client")
    public ClientProfileDto updateClientProfile(Long clientId, UpdateClientDto updateDto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        ApplicationUser user = client.getUser();

        // Update user information
        if (updateDto.getFirstName() != null)
            user.setFirstName(updateDto.getFirstName());
        if (updateDto.getLastName() != null)
            user.setLastName(updateDto.getLastName());

        if (updateDto.getEmail() != null && !updateDto.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateDto.getEmail())) {
                throw new RuntimeException("Cet email est déjà utilisé");
            }
            user.setEmail(updateDto.getEmail());
            // Penser à mettre à jour l'email du client aussi pour la cohérence
            client.setEmail(updateDto.getEmail());
        }

        // ✅ SYNCHRONISATION IMAGE DE PROFIL
        if (updateDto.getProfilePicture() != null) {
            user.setProfileImageUrl(updateDto.getProfilePicture());
            client.setProfileImageUrl(updateDto.getProfilePicture()); // <--- AJOUT ICI
        }

        // Update client information
        if (updateDto.getPhoneNumber() != null)
            client.setPhone(updateDto.getPhoneNumber());
        if (updateDto.getAddress() != null)
            client.setAddress(updateDto.getAddress());
        if (updateDto.getCity() != null)
            client.setCity(updateDto.getCity());
        if (updateDto.getPostalCode() != null)
            client.setPostalCode(updateDto.getPostalCode());
        if (updateDto.getBirthDate() != null)
            client.setBirthDate(updateDto.getBirthDate());
        if (updateDto.getPreferences() != null)
            client.setPreferences(updateDto.getPreferences());

        userRepository.save(user);
        clientRepository.save(client);

        log.info("Client profile updated: {}", clientId);
        return getClientProfile(clientId);
    }

    /**
     * Get client dashboard statistics
     */
    @Override
    public ClientDashboardStatsDto getDashboardStats(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));

        // Réservations SALON (modèle unique de réservation)
        List<SalonBooking> salonBookings = salonBookingRepository.findByClient_IdOrderByStartAtDesc(clientId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        long upcomingCount = salonBookings.stream()
                .filter(b -> b.getStartAt() != null && !b.getStartAt().isBefore(now))
                .filter(b -> b.getStatus() == BookingStatus.PENDING || b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.BOOKED)
                .count();
        long completedCount = salonBookings.stream().filter(b -> b.getStatus() == BookingStatus.COMPLETED).count();
        long cancelledCount = salonBookings.stream().filter(b -> b.getStatus() == BookingStatus.CANCELLED).count();
        long totalCount = salonBookings.size();

        // Favorites and reviews
        int favoritesCount = clientFavoriteRepository.countByClientId(clientId).intValue();
        int reviewsCount = (int) salonReviewRepository.countByClient_Id(clientId);

        // Loyalty tier calculation
        int loyaltyPoints = client.getLoyaltyPoints();
        String loyaltyTier = calculateLoyaltyTier(loyaltyPoints);
        int pointsToNextTier = calculatePointsToNextTier(loyaltyPoints);

        // Most visited barber
        ClientFavorite topFavorite = clientFavoriteRepository.findTopFavoritesByClientId(clientId)
                .stream()
                .findFirst()
                .orElse(null);

        String mostVisitedBarberName = null;
        Long mostVisitedBarberVisitCount = 0L;
        if (topFavorite != null) {
            Barber barber = topFavorite.getBarber();
            mostVisitedBarberName = barber.getUser().getFirstName() + " " + barber.getUser().getLastName();
            mostVisitedBarberVisitCount = topFavorite.getVisitCount().longValue();
        }

        // Total dépensé : prestations salon terminées
        Double totalSpent = salonBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED && b.getTotalPrice() != null)
                .mapToDouble(b -> b.getTotalPrice().doubleValue())
                .sum();
        Double averagePrice = totalSpent != null && completedCount > 0
                ? totalSpent / completedCount
                : 0.0;

        return ClientDashboardStatsDto.builder()
                .upcomingBookingsCount(upcomingCount)
                .completedBookingsCount(completedCount)
                .cancelledBookingsCount(cancelledCount)
                .totalBookingsCount(totalCount)
                .favoriteBarbersCount(favoritesCount)
                .reviewsLeftCount(reviewsCount)
                .loyaltyPoints(loyaltyPoints)
                .loyaltyTier(loyaltyTier)
                .pointsToNextTier(pointsToNextTier)
                .mostVisitedBarberName(mostVisitedBarberName)
                .mostVisitedBarberVisitCount(mostVisitedBarberVisitCount)
                .totalSpent(totalSpent != null ? totalSpent : 0.0)
                .averageBookingPrice(averagePrice)
                .build();
    }

    /**
     * Add barber to favorites
     */
    @Override
    @Transactional
    @LogActivity(action = "ADD_FAVORITE", entityName = "Client")
    public void addFavorite(Long clientId, Long barberId) {
        // Check if already favorited
        if (clientFavoriteRepository.existsByClientIdAndBarberId(clientId, barberId)) {
            throw new RuntimeException("Ce barbier est déjà dans vos favoris");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new RuntimeException("Client non trouvé"));
        Barber barber = barberRepository.findById(barberId)
                .orElseThrow(() -> new RuntimeException("Barbier non trouvé"));

        ClientFavorite favorite = ClientFavorite.builder()
                .client(client)
                .barber(barber)
                .visitCount(0)
                .build();

        clientFavoriteRepository.save(favorite);
        log.info("Barber {} added to favorites for client {}", barberId, clientId);
    }

    /**
     * Remove barber from favorites
     */
    @Override
    @Transactional
    @LogActivity(action = "REMOVE_FAVORITE", entityName = "Client")
    public void removeFavorite(Long clientId, Long barberId) {
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException("Client non trouvé: " + clientId);
        }

        if (!barberRepository.existsById(barberId)) {
            throw new ResourceNotFoundException("Barbier non trouvé: " + barberId);
        }
        clientFavoriteRepository.deleteByClientIdAndBarberId(clientId, barberId);
        log.info("Barber {} removed from favorites for client {}", barberId, clientId);
    }

    /**
     * Check if barber is favorited
     */
    @Override
    public boolean isFavorite(Long clientId, Long barberId) {
        return clientFavoriteRepository.existsByClientIdAndBarberId(clientId, barberId);
    }

    // Helper methods for loyalty tier calculation
    private String calculateLoyaltyTier(int points) {
        if (points >= 1000)
            return "Platinum";
        if (points >= 500)
            return "Gold";
        if (points >= 250)
            return "Silver";
        return "Bronze";
    }

    private int calculatePointsToNextTier(int points) {
        if (points >= 1000)
            return 0; // Already at max tier
        if (points >= 500)
            return 1000 - points;
        if (points >= 250)
            return 500 - points;
        return 250 - points;
    }

    @Override
    public List<BarberDto> getFavoriteBarbers(Long clientId) {

        // 1️⃣ Vérifier que le client existe
        if (!clientRepository.existsById(clientId)) {
            throw new ResourceNotFoundException(
                    "Client non trouvé avec l'ID: " + clientId);
        }

        // 2️⃣ Récupérer les favoris (avec JOIN FETCH barber recommandé côté repository)
        List<ClientFavorite> favorites = clientFavoriteRepository.findByClientId(clientId);

        // 3️⃣ Mapping manuel via BarberDto.fromEntity
        return favorites.stream()
                .map(ClientFavorite::getBarber) // récupérer l’entité Barber
                .filter(Objects::nonNull) // sécurité
                .map(BarberDto::fromEntity) // mapping manuel
                .collect(Collectors.toList());
    }

    @Override
    public List<ClientDto> getClientsByBarberId(Long barberId) {
        List<Client> clients = clientRepository.findClientsByBarberId(barberId);

        // On mappe vers DTO
        return clients.stream().map(client -> {
            ClientDto dto = ClientDto.fromEntity(client);

            // BONUS : On peut injecter des stats spécifiques à ce barbier ici si on veut
            // Ex: combien de fois ce client est venu chez CE barbier
            long visits = client.getBookings().stream()
                    .filter(b -> b.getBarber().getId().equals(barberId) && b.getStatus() == BookingStatus.COMPLETED)
                    .count();

            // On peut utiliser un champ transient dans le DTO ou juste renvoyer le client
            // brut
            // Pour l'instant on renvoie le client standard
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<BarberClientSummaryDto> getBarberClientsSummary(Long barberId) {
        return clientRepository.findClientSummariesByBarberId(barberId);
    }

    @Override
    public List<ClientDto> getRegularClientsByBarberId(Long barberId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRegularClientsByBarberId'");
    }

    @Override
    public Long countNewClientsSince(LocalDateTime date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'countNewClientsSince'");
    }

    @Override
    public Long countActiveClientsSince(LocalDateTime date) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'countActiveClientsSince'");
    }

}