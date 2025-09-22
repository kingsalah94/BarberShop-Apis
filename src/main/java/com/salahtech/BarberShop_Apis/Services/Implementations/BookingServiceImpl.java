package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.BookingConflictException;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.OutboxService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.Validators.BookingValidator;
import com.salahtech.BarberShop_Apis.domain.events.BookingCancelledEvent;
import com.salahtech.BarberShop_Apis.domain.events.BookingConfirmedEvent;
import com.salahtech.BarberShop_Apis.domain.events.BookingCreatedEvent;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import com.salahtech.BarberShop_Apis.reppsitories.ClientRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
@Transactional(readOnly = true)
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final BarberRepository barberRepository;
    private final AvailabilityServiceImpl availabilityService;
    private final BarberServiceRepository serviceRepository;
    private final ApplicationEventPublisher publisher; //
    private final OutboxService outboxService; // Service pour gérer l'outbox pattern

    // Ajouter une méthode de validation centralisée
    private void validateBookingSlot(Booking booking) {
        // Vérifier les disponibilités du barbier
        boolean isAvailable = availabilityService.isTimeSlotAvailable(
                booking.getBarber().getId(),
                booking.getStartAt(),
                booking.getEndAt());

        if (!isAvailable) {
            throw new BookingConflictException("Le créneau n'est pas disponible");
        }

        // Vérifier les conflits avec d'autres réservations
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                booking.getBarber().getId(),
                booking.getStartAt(),
                booking.getEndAt());

        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("Conflit avec une autre réservation");
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "barbers", key = "#dto.id")
    public BookingDto save(BookingDto dto) {

        // Charger les entités liées
        Client client = clientRepository.findById(dto.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        Barber barber = barberRepository.findById(dto.getBarberId())
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found"));
        BarberService service = serviceRepository.findById(dto.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service not found"));

        Booking booking = BookingDto.toEntity(dto);
        booking.setClient(client);
        booking.setBarber(barber);
        booking.setBarberService(service);
        booking.setStartAt(dto.getStartAt());
        booking.setEndAt(dto.getEndAt());
        booking.setNotes(dto.getNotes());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(service.getPrice());

        // ✅ AJOUTEZ CETTE LIGNE - Initialiser booking_date
        booking.setBookingDate(dto.getStartAt()); // Utiliser startAt comme date de réservation
        // OU si vous voulez la date sans l'heure :

        // Timestamps
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        // Validation métier
        validateBookingSlot(booking);

        booking = bookingRepository.save(booking);

        // // après save(booking)
        // outboxService.add(
        // "BOOKING",
        // booking.getId().toString(),
        // "BOOKING_CREATED",
        // Map.of(
        // "bookingId", booking.getId(),
        // "userId", booking.getBarber().getId(),
        // "barberId", booking.getBarber().getId(),
        // "eventAt", java.time.OffsetDateTime.now().toString()),
        // Map.of("corrId", java.util.UUID.randomUUID().toString()));
        // publisher.publishEvent(new BookingCreatedEvent(booking.getId()));
        return BookingDto.fromEntity(booking);
    }

    @Override
    @Cacheable(value = "barbers", key = "#id")
    public BookingDto findById(Long id) {
        return bookingRepository.findById(id)
                .map(BookingDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucune réservation trouvée avec l'ID = " + id,
                        ErrorCodes.BOOKING_NOT_FOUND));
    }

    @Override
    public List<BookingDto> findAll() {
        return bookingRepository.findAll().stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        if (id == null) {
            log.error("ID de réservation est null");
            return;
        }
        bookingRepository.deleteById(id);
    }

    // @Override
    // public List<BookingDto> findByUserId(Long userId) {
    // return bookingRepository.findByUserId(userId).stream()
    // .map(BookingDto::fromEntity)
    // .collect(Collectors.toList());
    // }

    // @Override
    // public List<BookingDto> findByBarberId(Long barberId) {
    // return bookingRepository.findByBarberId(barberId).stream()
    // .map(BookingDto::fromEntity)
    // .collect(Collectors.toList());
    // }

    @Override
    public List<BookingDto> findByServiceId(Long serviceId) {
        return bookingRepository.findByBarberService_Id(serviceId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> findByStatus(String status) {
        BookingStatus bookingStatus;
        try {
            bookingStatus = BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status invalide : " + status);
        }
        return bookingRepository.findByStatus(bookingStatus).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> findByBarberIdBetweenDates(Long barberId, LocalDateTime start, LocalDateTime end) {
        return bookingRepository.findConflictingBookings(barberId, start, end).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public Long countCompletedByBarber(Long barberId) {
        return bookingRepository.countCompletedBookingsByBarberId(barberId);
    }

    // =======================
    // Nouvelles opérations domaine (confirm / cancel)
    // =======================

    public BookingDto confirm(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalideEntityException("Réservation introuvable: " + bookingId,
                        ErrorCodes.BOOKING_NOT_FOUND));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        // après save(booking)
        outboxService.add(
                "BOOKING",
                booking.getId().toString(),
                "BOOKING_CONFIRMED",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getBarber().getId(),
                        "barberId", booking.getBarber().getId(),
                        "eventAt", java.time.OffsetDateTime.now().toString()),
                Map.of("corrId", java.util.UUID.randomUUID().toString()));
        publisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
        return BookingDto.fromEntity(booking);
    }

    public void cancel(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new InvalideEntityException("Réservation introuvable: " + bookingId,
                        ErrorCodes.BOOKING_NOT_FOUND));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        // après save(booking)
        outboxService.add(
                "BOOKING",
                booking.getId().toString(),
                "BOOKING_CANCELED",
                Map.of(
                        "bookingId", booking.getId(),
                        "userId", booking.getBarber().getId(),
                        "barberId", booking.getBarber().getId(),
                        "eventAt", java.time.OffsetDateTime.now().toString()),
                Map.of("corrId", java.util.UUID.randomUUID().toString()));
        publisher.publishEvent(new BookingCancelledEvent(booking.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> findByClientId(Long clientId) {
        if (clientId == null)
            return List.of();
        return bookingRepository.findByClient_Id(clientId)
                .stream()
                .map(BookingDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> findByClientUserId(Long userId) {
        if (userId == null)
            return List.of();
        return bookingRepository.findByClient_User_Id(userId)
                .stream()
                .map(BookingDto::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingDto> findByBarberId(Long barberId) {
        if (barberId == null)
            return List.of();
        return bookingRepository.findByBarber_Id(barberId)
                .stream()
                .map(BookingDto::fromEntity)
                .toList();
    }

}
