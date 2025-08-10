package com.salahtech.BarberShop_Apis.Services.Implementations;




import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Services.OutboxService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.domain.events.BookingCancelledEvent;
import com.salahtech.BarberShop_Apis.domain.events.BookingConfirmedEvent;
import com.salahtech.BarberShop_Apis.domain.events.BookingCreatedEvent;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher publisher; //
    private final OutboxService outboxService; // Service pour gérer l'outbox pattern

    @Override
    public BookingDto save(BookingDto dto) {
        Booking booking = BookingDto.toEntity(dto);
        if (booking.getCreatedAt() == null) {
            booking.setCreatedAt(LocalDateTime.now());
        }
        if (booking.getStatus() == null) {
            booking.setStatus(BookingStatus.PENDING);
        }
        booking = bookingRepository.save(booking);
        // après save(booking)
        outboxService.add(
        "BOOKING",
        booking.getId().toString(),
        "BOOKING_CREATED",
        Map.of(
            "bookingId", booking.getId(),
            "userId", booking.getUser().getId(),
            "barberId", booking.getBarber().getId(),
            "eventAt", java.time.OffsetDateTime.now().toString()
        ),
        Map.of("corrId", java.util.UUID.randomUUID().toString())
        );
        publisher.publishEvent(new BookingCreatedEvent(booking.getId()));
        return BookingDto.fromEntity(booking);
    }

    @Override
    public BookingDto findById(Long id) {
        return bookingRepository.findById(id)
                .map(BookingDto::fromEntity)
                .orElseThrow(() -> new InvalideEntityException(
                        "Aucune réservation trouvée avec l'ID = " + id,
                        ErrorCodes.BOOKING_NOT_FOUND
                ));
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

    @Override
    public List<BookingDto> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> findByBarberId(Long barberId) {
        return bookingRepository.findByBarberId(barberId).stream()
                .map(BookingDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<BookingDto> findByServiceId(Long serviceId) {
        return bookingRepository.findByBarberServiceId(serviceId).stream()
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
                    .orElseThrow(() -> new InvalideEntityException("Réservation introuvable: " + bookingId, ErrorCodes.BOOKING_NOT_FOUND));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);
        // après save(booking)
        outboxService.add(
        "BOOKING",
        booking.getId().toString(),
        "BOOKING_CONFIRMED",
        Map.of(
            "bookingId", booking.getId(),
            "userId", booking.getUser().getId(),
            "barberId", booking.getBarber().getId(),
            "eventAt", java.time.OffsetDateTime.now().toString()
        ),
        Map.of("corrId", java.util.UUID.randomUUID().toString())
        );
        publisher.publishEvent(new BookingConfirmedEvent(booking.getId()));
        return BookingDto.fromEntity(booking);
    }

     public void cancel(Long bookingId) {
        var booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new InvalideEntityException("Réservation introuvable: " + bookingId, ErrorCodes.BOOKING_NOT_FOUND));
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        // après save(booking)
        outboxService.add(
        "BOOKING",
        booking.getId().toString(),
        "BOOKING_CANCELED",
        Map.of(
            "bookingId", booking.getId(),
            "userId", booking.getUser().getId(),
            "barberId", booking.getBarber().getId(),
            "eventAt", java.time.OffsetDateTime.now().toString()
        ),
        Map.of("corrId", java.util.UUID.randomUUID().toString())
        );
        publisher.publishEvent(new BookingCancelledEvent(booking.getId()));
    }
}

