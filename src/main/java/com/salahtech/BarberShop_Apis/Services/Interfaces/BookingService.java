package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingService {

    BookingDto save(BookingDto dto);

    BookingDto findById(Long id);

    List<BookingDto> findAll();

    void delete(Long id);

    List<BookingDto> findByUserId(Long userId);

    List<BookingDto> findByBarberId(Long barberId);

    List<BookingDto> findByServiceId(Long serviceId);

    List<BookingDto> findByStatus(String status);

    List<BookingDto> findByBarberIdBetweenDates(Long barberId, LocalDateTime start, LocalDateTime end);

    Long countCompletedByBarber(Long barberId);
}

