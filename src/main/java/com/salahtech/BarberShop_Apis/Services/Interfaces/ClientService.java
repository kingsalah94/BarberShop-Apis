package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.ClientDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientProfileDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateClientDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberClientSummaryDto;
import com.salahtech.BarberShop_Apis.Dtos.BarberDto;
import com.salahtech.BarberShop_Apis.Dtos.ClientDashboardStatsDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

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

        /**
         * Get client profile with all information
         */
        ClientProfileDto getClientProfile(Long clientId);

        /**
         * Update client profile
         */
        ClientProfileDto updateClientProfile(Long clientId, UpdateClientDto updateDto);

        /**
         * Get client dashboard statistics
         */
        ClientDashboardStatsDto getDashboardStats(Long clientId);

        ClientDto create(ClientDto clientDto, MultipartFile file);

        ClientDto update(Long id, ClientDto clientDto, MultipartFile file);

        Long countNewClientsSince(LocalDateTime date);

}