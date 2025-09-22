package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.BookingDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ClientService {

    // CRUD de base
    ClientDto save(ClientDto dto);

    ClientDto findById(Long id);

    ClientDto findByUserId(Long userId);

    ClientDto findByEmail(String email);

    ClientDto findByPhone(String phone);

    List<ClientDto> findAll();

    void delete(Long id);

    // Recherche avancée
    Page<ClientDto> search(String query, Boolean hasBookings, LocalDateTime createdFrom,
            LocalDateTime createdTo, Pageable pageable);

    // Gestion des bookings du client
    List<BookingDto> getClientBookings(Long clientId);

    List<BookingDto> getClientBookingsByStatus(Long clientId, String status);

    BookingDto getLastBooking(Long clientId);

    Integer countClientBookings(Long clientId);

    // Mise à jour partielle
    ClientDto updatePreferences(Long clientId, String preferences);

    ClientDto updateNotes(Long clientId, String notes);

    ClientDto updateContact(Long clientId, String email, String phone);

    // Statistiques
    ClientStatsDto getClientStats(Long clientId);

    List<ClientDto> getTopClients(int limit);

    List<ClientDto> getInactiveClients(int daysInactive);

    // Validation
    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    boolean existsByUserId(Long userId);

    // DTO pour les statistiques
    record ClientStatsDto(
            Long clientId,
            Integer totalBookings,
            Integer completedBookings,
            Integer cancelledBookings,
            Double totalSpent,
            LocalDateTime firstBooking,
            LocalDateTime lastBooking,
            Double averageRating,
            List<String> favoriteServices) {
    }
}