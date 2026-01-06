package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salahtech.BarberShop_Apis.Dtos.*;
import com.salahtech.BarberShop_Apis.Enums.*;
import com.salahtech.BarberShop_Apis.Exceptions.*;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.models.*;
import com.salahtech.BarberShop_Apis.outbox.OutboxEvent;
import com.salahtech.BarberShop_Apis.outbox.OutboxStatus;
import com.salahtech.BarberShop_Apis.reppsitories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;
    private final BarberServiceRepository serviceRepository;
    private final ApplicationUserRepository applicationUserRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    // ========================================================================
    // 📝 1. CRÉATION (CLIENT)
    // ========================================================================

    @Override
    @Transactional
    @CacheEvict(value = "barbers", key = "#dto.barberId")
    public BookingDto createBooking(BookingCreateDto dto) {

        // =================================================================
        // 🚨 CORRECTION : CHOIX DU CLIENT (Admin vs Client connecté)
        // =================================================================

        Client client;

        // 1. Si un ID Client est fourni (Cas Admin qui réserve pour "Jean Dupont")
        if (dto.getClientId() != null) {
            Long receivedId = dto.getClientId(); // ex: 3 (User ID)

            // On essaie d'abord de trouver par User ID (Le cas le plus probable ici)
            client = clientRepository.findByUserId(receivedId)
                    // Si pas trouvé, on essaie par Client ID (Fallback)
                    .or(() -> clientRepository.findById(receivedId))
                    // Si toujours rien, erreur
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Client introuvable (ni par UserID, ni par ClientID : " + receivedId + ")"));

        }
        // 2. Sinon, on utilise l'utilisateur connecté (Cas Client qui réserve pour
        // lui-même)
        else {
            ApplicationUser user = getCurrentUser();
            // Petite sécurité : Si c'est un Admin qui a oublié de choisir un client, on
            // bloque
            if (user.getUserType() == ApplicationUserType.ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "En tant qu'Admin, vous devez sélectionner un client.");
            }
            client = getOrCreateClientProfile(user);
        }

        // 2. Validation des Entrées (Barbier & Service)
        Barber barber = barberRepository.findById(dto.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable"));

        BarberService service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service introuvable"));

        // 3. Calcul des horaires
        LocalDateTime startAt = dto.getStartAt();
        // CORRECTION MINEURE : Si endAt est fourni dans le DTO, on l'utilise, sinon on
        // calcule
        LocalDateTime endAt;
        if (dto.getEndAt() != null) {
            endAt = dto.getEndAt();
        } else {
            endAt = startAt.plusMinutes(service.getDuration());
        }

        // 4. 🔒 VÉRIFICATION ANTI-DOUBLON (Lock Pessimiste en DB recommandé ici)
        Long conflicts = bookingRepository.countConflictingBookingsWithLock(barber.getId(), startAt, endAt);
        if (conflicts > 0) {
            throw new BookingConflictException("Ce créneau est déjà réservé.");
        }

        // 5. Construction de l'entité
        Booking booking = Booking.builder()
                .client(client)
                .barber(barber)
                .barberService(service)
                .startAt(startAt)
                .endAt(endAt)
                .bookingDate(startAt)
                .status(BookingStatus.PENDING)
                .totalPrice(service.getPrice())
                .notes(dto.getNotes())
                .paymentMethod(dto.getPaymentMethod() != null ? dto.getPaymentMethod() : PaymentMethod.CASH)
                .paymentStatus(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        // 6. Sauvegarde
        Booking saved = bookingRepository.save(booking);

        // 7. Notification Outbox (New Booking)
        notifyChange(saved, "BOOKING_CREATED", "Nouvelle réservation",
                "Nouvelle demande de " + client.getFirstName());

        return mapToDto(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "barbers", key = "#result.barberId") // On vide le cache du barbier concerné
    public BookingDto updateBooking(Long id, BookingDto dto) {

        // 1. Charger la réservation existante
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable avec l'ID : " + id));

        // On garde l'ancien état pour comparer (pour les notifs)
        LocalDateTime oldStart = booking.getStartAt();
        BookingStatus oldStatus = booking.getStatus();

        boolean isRescheduled = false;

        // =================================================================
        // 1. CHANGEMENT DE CLIENT (Admin uniquement)
        // =================================================================
        // Si un clientId est fourni ET qu'il est différent de l'actuel
        if (dto.getClientId() != null) {
            // On vérifie si l'ID reçu correspond au User ID du client actuel
            boolean isSameUser = booking.getClient() != null &&
                    booking.getClient().getUser().getId().equals(dto.getClientId());

            // Si c'est différent, on cherche le nouveau client
            if (!isSameUser) {
                Long receivedId = dto.getClientId();
                Client newClient = clientRepository.findByUserId(receivedId)
                        .or(() -> clientRepository.findById(receivedId))
                        .orElseThrow(() -> new ResourceNotFoundException("Client introuvable ID: " + receivedId));

                booking.setClient(newClient);
            }
        }

        // =================================================================
        // 2. CHANGEMENT DE BARBIER
        // =================================================================
        if (dto.getBarberId() != null && !booking.getBarber().getId().equals(dto.getBarberId())) {
            Barber newBarber = barberRepository.findById(dto.getBarberId())
                    .orElseThrow(() -> new ResourceNotFoundException("Barbier introuvable ID: " + dto.getBarberId()));
            booking.setBarber(newBarber);
            isRescheduled = true; // Changer de barbier = changement majeur
        }

        // =================================================================
        // 3. CHANGEMENT DE DATE (Avec exclusion de soi-même)
        // =================================================================
        if (dto.getStartAt() != null && !booking.getStartAt().equals(dto.getStartAt())) {

            LocalDateTime newStart = dto.getStartAt();
            LocalDateTime newEnd;

            // Recalcul de la fin
            if (dto.getEndAt() != null) {
                newEnd = dto.getEndAt();
            } else {
                // On garde la durée actuelle du service (ou on recharge le service pour être
                // sûr)
                int duration = booking.getBarberService().getDuration();
                newEnd = newStart.plusMinutes(duration);
            }

            // VÉRIFICATION DE CONFLIT (EXCLURE L'ID ACTUEL)
            Long conflicts = bookingRepository.countConflictingBookingsExcludeId(
                    booking.getBarber().getId(),
                    newStart,
                    newEnd,
                    id // ID exclu
            );

            if (conflicts > 0) {
                throw new BookingConflictException("Ce créneau est déjà réservé pour ce barbier.");
            }

            booking.setStartAt(newStart);
            booking.setEndAt(newEnd);
            booking.setBookingDate(newStart); // Mise à jour index date
            isRescheduled = true;
        }

        // =================================================================
        // 📝 4. AUTRES CHAMPS
        // =================================================================
        if (dto.getNotes() != null)
            booking.setNotes(dto.getNotes());

        if (dto.getStatus() != null) {
            booking.setStatus(dto.getStatus());
        }

        if (dto.getTotalPrice() != null)
            booking.setTotalPrice(dto.getTotalPrice());

        if (dto.getPaymentMethod() != null)
            booking.setPaymentMethod(dto.getPaymentMethod());
        if (dto.getPaymentStatus() != null)
            booking.setPaymentStatus(dto.getPaymentStatus());

        // 5. Sauvegarde
        Booking saved = bookingRepository.save(booking);

        // =================================================================
        // 🔔 6. NOTIFICATION & OUTBOX
        // =================================================================
        // On détecte quel type d'événement générer

        if (isRescheduled) {
            // Cas : Déplacement du RDV
            // ✅ CORRECTION : On utilise 'oldStart' pour donner plus de détails
            String messageDetails = String.format("Votre rendez-vous du %s a été déplacé au %s.",
                    oldStart.toString(), // Ancienne date (variable utilisée !)
                    saved.getStartAt().toString() // Nouvelle date
            );

            notifyChange(saved, "BOOKING_RESCHEDULED", "Rendez-vous déplacé", messageDetails);

        } else if (saved.getStatus() != oldStatus) {
            // Cas : Changement de statut
            String eventType = switch (saved.getStatus()) {
                case CONFIRMED -> "BOOKING_CONFIRMED";
                case CANCELLED -> "BOOKING_CANCELLED";
                case COMPLETED -> "BOOKING_COMPLETED";
                default -> "BOOKING_UPDATED";
            };

            String message = switch (saved.getStatus()) {
                case CONFIRMED -> "Votre rendez-vous est confirmé.";
                case CANCELLED -> "Votre rendez-vous a été annulé.";
                case COMPLETED -> "Merci de votre visite !";
                default -> "Le statut de votre rendez-vous a changé.";
            };

            notifyChange(saved, eventType, "Mise à jour RDV", message);

        } else {
            // Mise à jour mineure (Notes, Prix...) -> Pas forcément de notif push,
            // mais on peut logger l'event pour l'audit.
            // notifyChange(saved, "BOOKING_UPDATED", "Mise à jour", "Détails mis à jour");
        }

        return BookingDto.fromEntity(saved);
    }

    // ========================================================================
    // ✅ 2. CONFIRMATION (BARBIER)
    // ========================================================================

    @Override
    @Transactional
    public BookingDto confirmBooking(Long bookingId, Long barberId) {
        Booking booking = getBookingOrThrow(bookingId);

        // Sécurité : Seul le barbier concerné peut confirmer
        if (!booking.getBarber().getId().equals(barberId)) {
            throw new InvalideEntityException("Action non autorisée.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new InvalideEntityException("Statut invalide pour confirmation.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        Booking saved = bookingRepository.save(booking);

        // Notif Client
        notifyChange(saved, "BOOKING_CONFIRMED", "Réservation Confirmée ✅",
                "Votre RDV chez " + saved.getBarber().getSalonName() + " est validé.");

        return mapToDto(saved);
    }

    // ========================================================================
    // ❌ 3. ANNULATION (CLIENT OU BARBIER)
    // ========================================================================

    @Override
    @Transactional
    public BookingDto cancelBooking(Long bookingId, Long userId) {
        Booking booking = getBookingOrThrow(bookingId);

        boolean isClient = booking.getClient().getUser().getId().equals(userId);
        boolean isBarber = booking.getBarber().getUser().getId().equals(userId);

        if (!isClient && !isBarber) {
            throw new InvalideEntityException("Action non autorisée.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalideEntityException("Déjà annulé.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking saved = bookingRepository.save(booking);

        // Notif à la contrepartie
        String msg = isClient ? "Le client a annulé." : "Le barbier a annulé.";
        notifyChange(saved, "BOOKING_CANCELLED", "Rendez-vous Annulé ❌", msg);

        return mapToDto(saved);
    }

    // ========================================================================
    // ⭐ 4. TERMINAISON (BARBIER)
    // ========================================================================

    @Override
    @Transactional
    public BookingDto completeBooking(Long bookingId, Long barberId) {
        Booking booking = getBookingOrThrow(bookingId);

        if (!booking.getBarber().getId().equals(barberId)) {
            throw new InvalideEntityException("Action non autorisée.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalideEntityException("Le RDV doit être confirmé avant d'être terminé.");
        }

        booking.setStatus(BookingStatus.COMPLETED);

        // Paiement Auto (Cash)
        if (booking.getPaymentMethod() == PaymentMethod.CASH && booking.getPaymentStatus() == PaymentStatus.PENDING) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        }

        Booking saved = bookingRepository.save(booking);

        // Notif Client (Demande avis)
        notifyChange(saved, "BOOKING_COMPLETED", "Merci pour votre visite ⭐",
                "N'oubliez pas de noter votre prestation !");

        return mapToDto(saved);
    }

    // ========================================================================
    // 🔍 5. LECTURE & HELPERS
    // ========================================================================

    @Override
    @Cacheable(value = "bookings", key = "#id")
    public BookingDto findById(Long id) {
        return bookingRepository.findById(id).map(this::mapToDto)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
    }

    // ... (Vos autres méthodes findByClientId, findByBarberId restent identiques,
    // je les abrège pour la clarté) ...

    @Override
    public List<BookingDto> findByBarberId(Long barberId) {
        if (barberId == null)
            return List.of();
        return bookingRepository.findByBarber_Id(barberId).stream().map(this::mapToDto).toList();
    }

    @Override
    public List<BookingDto> findByClientId(Long clientId) {
        if (clientId == null)
            return List.of();
        return bookingRepository.findByClient_Id(clientId).stream().map(this::mapToDto).toList();
    }

    @Override
    public List<BookingDto> findAll() {
        return List.of();
    } // À éviter en prod sur grosse table

    @Override
    public void delete(Long id) {
        bookingRepository.deleteById(id);
    }

    // @Override
    // public BookingDto save(BookingDto dto) {
    // return null;
    // } // Obsolète, utilisez createBooking

    @Override
    public List<BookingDto> findByServiceId(Long id) {
        return List.of();
    }

    @Override
    public List<BookingDto> findByStatus(String s) {
        return List.of();
    }

    @Override
    public List<BookingDto> findByBarberIdBetweenDates(Long b, LocalDateTime s, LocalDateTime e) {
        return List.of();
    }

    @Override
    public Long countCompletedByBarber(Long b) {
        return 0L;
    }

    @Override
    public BookingDto pendBooking(Long id) {
        return null;
    } // Obsolète

    @Override
    public void cancel(Long id) {
        cancelBooking(id, getCurrentUser().getId());
    } // Proxy

    @Override
    public List<BookingDto> findByClientUserId(Long userId) {
        return bookingRepository.findByClient_User_Id(userId).stream().map(this::mapToDto).toList();
    }

    // ========================================================================
    // 🛠️ MÉTHODES PRIVÉES (DRY)
    // ========================================================================

    private Booking getBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Réservation introuvable (ID: " + id + ")"));
    }

    private ApplicationUser getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return applicationUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    private Client getOrCreateClientProfile(ApplicationUser user) {
        if (user.getClient() != null)
            return user.getClient();

        Client newClient = Client.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .build();
        return clientRepository.save(newClient);
    }

    /**
     * 🔥 Méthode Centrale pour l'Outbox Pattern (DRY)
     */
    private void notifyChange(Booking booking, String eventType, String title, String body) {
        try {
            // Déterminer le destinataire (Celui qui N'A PAS fait l'action)
            // Simplification : On envoie l'event Kafka générique, le Consumer décidera qui
            // notifier
            // Ou on stocke l'ID du destinataire principal dans le message
            Long targetUserId = eventType.equals("BOOKING_CREATED")
                    ? booking.getBarber().getUser().getId() // Pour le barbier
                    : booking.getClient().getUser().getId(); // Pour le client

            NotificationMessageDto msgDto = new NotificationMessageDto(
                    targetUserId, title, body, eventType, booking.getId());

            String payload = objectMapper.writeValueAsString(msgDto);

            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(String.valueOf(booking.getId()))
                    .aggregateType("BOOKING")
                    .eventType(eventType)
                    .payload(payload)
                    .createdAt(OffsetDateTime.now())
                    .status(OutboxStatus.PENDING)
                    .attempts(0)
                    .nextAttemptAt(OffsetDateTime.now().plusMinutes(1))
                    .updatedAt(OffsetDateTime.now())
                    .build();

            outboxEventRepository.save(event);

        } catch (Exception e) {
            log.error("Erreur création Outbox Event: {}", e.getMessage());
            // On n'annule pas la transaction principale pour une erreur de notif
        }
    }

    private BookingDto mapToDto(Booking b) {
        BookingDto dto = BookingDto.builder()
                .id(b.getId())
                .clientId(b.getClient().getId())
                .barberId(b.getBarber().getId())
                .serviceId(b.getBarberService().getId())
                .startAt(b.getStartAt())
                .endAt(b.getEndAt())
                .status(b.getStatus())
                .totalPrice(b.getTotalPrice())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .paymentMethod(b.getPaymentMethod())
                .paymentStatus(b.getPaymentStatus())
                .build();

        // Enrichissement rapide
        if (b.getBarber() != null)
            dto.setSalonName(b.getBarber().getSalonName());
        if (b.getBarberService() != null)
            dto.setServiceName(b.getBarberService().getName());
        if (b.getClient() != null) {
            dto.setClientFirstName(b.getClient().getFirstName());
            dto.setClientLastName(b.getClient().getLastName());
        }
        return dto;
    }

}