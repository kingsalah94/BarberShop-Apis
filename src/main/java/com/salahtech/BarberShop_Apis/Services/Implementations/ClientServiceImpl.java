package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Exceptions.ConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ClientService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;
    private final ApplicationUserRepository userRepository;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
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
    public ClientDto updatePreferences(Long clientId, String preferences) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));
        client.setPreferences(preferences);
        return ClientDto.fromEntity(clientRepository.save(client));
    }

    @Override
    @Transactional
    public ClientDto updateNotes(Long clientId, String notes) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client non trouvé"));
        client.setNotes(notes);
        return ClientDto.fromEntity(clientRepository.save(client));
    }

    @Override
    @Transactional
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
}