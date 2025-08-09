package com.salahtech.BarberShop_Apis.Services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.salahtech.BarberShop_Apis.Dtos.BarberSearchDto;
import com.salahtech.BarberShop_Apis.Dtos.ServiceSearchDto;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.BarberService;
import com.salahtech.BarberShop_Apis.reppsitories.BarberRepository;
import com.salahtech.BarberShop_Apis.reppsitories.BarberServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import lombok.extern.slf4j.Slf4j;

@Service
@Transactional(readOnly = true)
@Slf4j
public class SearchService {

    @Autowired
    private BarberRepository barberRepository;

    @Autowired
    private BarberServiceRepository serviceRepository;

    /**
     * Recherche avancée de barbiers
     */
    public Page<Barber> searchBarbers(BarberSearchDto searchDto, Pageable pageable) {
        log.info("Performing advanced barber search with criteria: {}", searchDto);

        List<Barber> allBarbers = barberRepository.findAll();
        List<Barber> filteredBarbers = new ArrayList<>(allBarbers);

        // Filtrer par disponibilité
        if (searchDto.getAvailableOnly() != null && searchDto.getAvailableOnly()) {
            filteredBarbers = filteredBarbers.stream()
                .filter(Barber::getAvailable)
                .collect(Collectors.toList());
        }

        // Filtrer par localisation
        if (searchDto.getLocation() != null && !searchDto.getLocation().trim().isEmpty()) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getLocation().toLowerCase()
                    .contains(searchDto.getLocation().toLowerCase()))
                .collect(Collectors.toList());
        }

        // Filtrer par coordonnées GPS et rayon
        if (searchDto.getLatitude() != null && searchDto.getLongitude() != null 
            && searchDto.getRadiusKm() != null) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getLatitude() != null && barber.getLongitude() != null)
                .filter(barber -> calculateDistance(
                    searchDto.getLatitude(), searchDto.getLongitude(),
                    barber.getLatitude(), barber.getLongitude()
                ) <= searchDto.getRadiusKm())
                .collect(Collectors.toList());
        }

        // Filtrer par gamme de prix
        if (searchDto.getMinPrice() != null) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getPriceFrom().compareTo(searchDto.getMinPrice()) >= 0)
                .collect(Collectors.toList());
        }
        if (searchDto.getMaxPrice() != null) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getPriceFrom().compareTo(searchDto.getMaxPrice()) <= 0)
                .collect(Collectors.toList());
        }

        // Filtrer par note minimale
        if (searchDto.getMinRating() != null) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getRating().compareTo(searchDto.getMinRating()) >= 0)
                .collect(Collectors.toList());
        }

        // Filtrer par nom de salon
        if (searchDto.getSalonName() != null && !searchDto.getSalonName().trim().isEmpty()) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getSalonName().toLowerCase()
                    .contains(searchDto.getSalonName().toLowerCase()))
                .collect(Collectors.toList());
        }

        // Filtrer par spécialité
        if (searchDto.getSpecialty() != null && !searchDto.getSpecialty().trim().isEmpty()) {
            filteredBarbers = filteredBarbers.stream()
                .filter(barber -> barber.getSpecialties() != null && 
                        !barber.getSpecialties().trim().isEmpty())
                .filter(barber -> {
                    String[] specialties = barber.getSpecialties().split(",");
                    return Arrays.stream(specialties)
                        .anyMatch(specialty -> specialty.trim().toLowerCase()
                            .contains(searchDto.getSpecialty().toLowerCase()));
                })
                .collect(Collectors.toList());
        }

        // Tri des résultats
        if (searchDto.getSortBy() != null) {
            filteredBarbers = sortBarbers(filteredBarbers, searchDto.getSortBy(), 
                searchDto.getSortDirection(), searchDto.getLatitude(), searchDto.getLongitude());
        }

        // Pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredBarbers.size());
        
        List<Barber> pageContent = start < filteredBarbers.size() 
            ? filteredBarbers.subList(start, end) 
            : new ArrayList<>();

        log.info("Search completed. Found {} barbers matching criteria", filteredBarbers.size());
        
        return new PageImpl<>(pageContent, pageable, filteredBarbers.size());
    }

    /**
     * Recherche avancée de services
     */
    public Page<BarberService> searchServices(
            ServiceSearchDto searchDto, Pageable pageable) {
        
        log.info("Performing advanced service search with criteria: {}", searchDto);

        List<BarberService> allBarberServices = serviceRepository.findAll();
        List<BarberService> filteredBarberServices = new ArrayList<>(allBarberServices);

        // Filtrer par statut actif
        if (searchDto.getActiveOnly() != null && searchDto.getActiveOnly()) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(BarberService::getActive)
                .collect(Collectors.toList());
        }

        // Filtrer par nom
        if (searchDto.getName() != null && !searchDto.getName().trim().isEmpty()) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getName().toLowerCase()
                    .contains(searchDto.getName().toLowerCase()))
                .collect(Collectors.toList());
        }

        // Filtrer par catégorie
        if (searchDto.getCategory() != null) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getCategory().equals(searchDto.getCategory()))
                .collect(Collectors.toList());
        }

        // Filtrer par gamme de prix
        if (searchDto.getMinPrice() != null) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getPrice().compareTo(searchDto.getMinPrice()) >= 0)
                .collect(Collectors.toList());
        }
        if (searchDto.getMaxPrice() != null) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getPrice().compareTo(searchDto.getMaxPrice()) <= 0)
                .collect(Collectors.toList());
        }

        // Filtrer par durée maximale
        if (searchDto.getMaxDuration() != null) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getDuration() <= searchDto.getMaxDuration())
                .collect(Collectors.toList());
        }

        // Filtrer par barbier
        if (searchDto.getBarberId() != null) {
            filteredBarberServices = filteredBarberServices.stream()
                .filter(service -> service.getBarber().getId().equals(searchDto.getBarberId()))
                .collect(Collectors.toList());
        }

        // Tri des résultats
        if (searchDto.getSortBy() != null) {
            filteredBarberServices = sortServices(filteredBarberServices, searchDto.getSortBy(),
                searchDto.getSortDirection());
        }

        // Pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredBarberServices.size());
        
        List<BarberService> pageContent = start < filteredBarberServices.size()
            ? filteredBarberServices.subList(start, end)
            : new ArrayList<>();

        log.info("Service search completed. Found {} services matching criteria", filteredBarberServices.size());
        
        return new PageImpl<>(pageContent, pageable, filteredBarberServices.size());
    }

    /**
     * Obtenir les barbiers populaires
     */
    public List<Barber> getPopularBarbers(int limit) {
        return barberRepository.findAllOrderByRatingDesc()
            .stream()
            .filter(Barber::getAvailable)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Obtenir les barbiers à proximité
     */
    public List<Barber> getNearbyBarbers(Double latitude, Double longitude, Double radiusKm, int limit) {
        List<Barber> allBarbers = barberRepository.findByAvailable(true);
        
        return allBarbers.stream()
            .filter(barber -> barber.getLatitude() != null && barber.getLongitude() != null)
            .filter(barber -> calculateDistance(latitude, longitude, 
                barber.getLatitude(), barber.getLongitude()) <= radiusKm)
            .sorted((b1, b2) -> {
                double d1 = calculateDistance(latitude, longitude, b1.getLatitude(), b1.getLongitude());
                double d2 = calculateDistance(latitude, longitude, b2.getLatitude(), b2.getLongitude());
                return Double.compare(d1, d2);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Obtenir des recommandations personnalisées
     */
    public List<Barber> getPersonalizedRecommendations(Long userId, int limit) {
        // Cette méthode pourrait utiliser l'historique des réservations de l'utilisateur
        // pour recommander des barbiers similaires ou dans des zones fréquentées
        // Pour l'instant, retourne les barbiers les mieux notés
        
        return getPopularBarbers(limit);
    }

    /**
     * Calculer la distance entre deux points GPS (en km)
     */
    // private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    //     final int R = 6371; // Rayon de la Terre en kilomètres

    //     double latDistance = Math.toRadians(lat2 - lat1);
    //     double lonDistance = Math.toRadians(lon2 - lon1);
    //     double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
    //             + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
    //             * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    //     double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    //     return R * c;
    // }

    private double calculateDistance(Object lat1, Object lon1, Object lat2, Object lon2) {
        try {
            double dLat1 = convertToDouble(lat1);
            double dLon1 = convertToDouble(lon1);
            double dLat2 = convertToDouble(lat2);
            double dLon2 = convertToDouble(lon2);
            
            // Votre calcul de distance existant...
            final int R = 6371;
            double latDistance = Math.toRadians(dLat2 - dLat1);
            double lonDistance = Math.toRadians(dLon2 - dLon1);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                    + Math.cos(Math.toRadians(dLat1)) * Math.cos(Math.toRadians(dLat2))
                    * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                
            return R * c;

        } catch (Exception e) {
            return Double.MAX_VALUE;
        }
    }

    private double convertToDouble(Object value) {
    if (value == null) throw new IllegalArgumentException("Null coordinate");
    if (value instanceof Double) return (Double) value;
    if (value instanceof BigDecimal) return ((BigDecimal) value).doubleValue();
    if (value instanceof Number) return ((Number) value).doubleValue();
    throw new IllegalArgumentException("Cannot convert to double: " + value.getClass());
}

    /**
     * Trier les barbiers selon les critères
     */
    private List<Barber> sortBarbers(List<Barber> barbers, String sortBy, String sortDirection,
            Double userLat, Double userLon) {
        
        Comparator<Barber> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "rating":
                comparator = Comparator.comparing(Barber::getRating);
                break;
            case "price":
                comparator = Comparator.comparing(Barber::getPriceFrom);
                break;
            case "name":
                comparator = Comparator.comparing(barber -> barber.getUser().getFirstName());
                break;
            case "distance":
                if (userLat != null && userLon != null) {
                    comparator = Comparator.comparing(barber -> {
                        if (barber.getLatitude() != null && barber.getLongitude() != null) {
                            return calculateDistance(userLat, userLon, 
                                barber.getLatitude(), barber.getLongitude());
                        }
                        return Double.MAX_VALUE;
                    });
                } else {
                    comparator = Comparator.comparing(Barber::getId);
                }
                break;
            default:
                comparator = Comparator.comparing(Barber::getId);
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return barbers.stream().sorted(comparator).collect(Collectors.toList());
    }

    /**
     * Trier les services selon les critères
     */
    private List<BarberService> sortServices(
            List<BarberService> barberServices, String sortBy, String sortDirection) {
        
        Comparator<BarberService> comparator;
        
        switch (sortBy.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(BarberService::getName);
                break;
            case "price":
                comparator = Comparator.comparing(BarberService::getPrice);
                break;
            case "duration":
                comparator = Comparator.comparing(BarberService::getDuration);
                break;
            case "category":
                comparator = Comparator.comparing(service -> service.getCategory().name());
                break;
            default:
                comparator = Comparator.comparing(BarberService::getId);
        }

        if ("desc".equalsIgnoreCase(sortDirection)) {
            comparator = comparator.reversed();
        }

        return barberServices.stream().sorted(comparator).collect(Collectors.toList());
    }
}