package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.BookingCreateDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingRequestDto;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    // BookingDto save(BookingDto dto);

    BookingDto findById(Long id);

    List<BookingDto> findAll();

    void delete(Long id);

    // BookingDto confirm(Long bookingId);

    BookingDto pendBooking(Long bookingId);

    // BookingDto completeBooking(Long bookingId);

    BookingDto confirmBooking(Long bookingId, Long barberId);

    BookingDto cancelBooking(Long bookingId, Long userId);

    void cancel(Long bookingId);

    /** ✅ désormais côté lecture on parle de client */
    List<BookingDto> findByClientId(Long clientId);

    /** ✅ pratique : via client.user.id */
    List<BookingDto> findByClientUserId(Long userId);

    List<BookingDto> findByBarberId(Long barberId);

    List<BookingDto> findByServiceId(Long serviceId);

    List<BookingDto> findByStatus(String status);

    /** ✅ fenêtres temporelles basées sur startAt/endAt */
    List<BookingDto> findByBarberIdBetweenDates(Long barberId, LocalDateTime start, LocalDateTime end);

    Long countCompletedByBarber(Long barberId);

    BookingDto createBooking(BookingCreateDto dto);

    BookingDto completeBooking(Long bookingId, Long barberId);

    // BookingDto updateBooking(Long id, BookingDto dto);

    BookingDto updateBooking(Long id, BookingDto dto);

    // BookingDto createBooking(BookingRequestDto bookingRequestDto);
}
