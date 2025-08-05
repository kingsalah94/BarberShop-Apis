package com.salahtech.BarberShop_Apis.Services.Implementations;




import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.ErrorCodes;
import com.salahtech.BarberShop_Apis.Exceptions.InvalideEntityException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.BookingService;
import com.salahtech.BarberShop_Apis.models.Booking;
import com.salahtech.BarberShop_Apis.reppsitories.BookingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;

    @Override
    public BookingDto save(BookingDto dto) {
        Booking booking = BookingDto.toEntity(dto);
        booking.setCreatedAt(LocalDateTime.now());
        return BookingDto.fromEntity(bookingRepository.save(booking));
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
        return bookingRepository.findByServiceId(serviceId).stream()
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
}

