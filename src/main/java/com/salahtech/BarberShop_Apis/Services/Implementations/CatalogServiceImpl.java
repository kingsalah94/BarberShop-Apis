package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDetailDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonCatalogDto;
import com.salahtech.BarberShop_Apis.Dtos.SalonPublicReviewDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeWindowDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.CatalogService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonOfferingService;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonOffering;
import com.salahtech.BarberShop_Apis.models.SalonReview;
import com.salahtech.BarberShop_Apis.repositories.SalonOfferingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogServiceImpl implements CatalogService {

    private final LocalityService localityService;
    private final SalonRepository salonRepository;
    private final SalonOfferingRepository offeringRepository;
    private final SalonReviewRepository reviewRepository;
    private final SalonOfferingService offeringService;
    private final SalonHoursService hoursService;

    /** Note moyenne arrondie à 1 décimale (null si aucun avis). */
    private Double ratingOf(Long salonId) {
        Double avg = reviewRepository.avgRating(salonId);
        return avg == null ? null : Math.round(avg * 10) / 10.0;
    }

    /** Données calculées communes à la liste et à la fiche. */
    record Extras(BigDecimal minPrice, List<String> categories,
                  boolean openNow, String openAt, String closeAt) {}

    Extras extras(Long salonId) {
        List<SalonOffering> active = offeringRepository.findBySalon_IdAndActiveTrueOrderByNameAsc(salonId);
        BigDecimal minPrice = active.stream().map(SalonOffering::getPrice)
                .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        List<String> categories = active.stream()
                .map(o -> o.getCategory() != null ? o.getCategory().getName() : null)
                .filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());

        boolean openNow = false;
        String openAt = null;
        String closeAt = null;
        try {
            EffectiveHoursDto today = hoursService.getEffectiveHours(salonId, LocalDate.now());
            if (today != null && !today.isClosed()) {
                LocalTime t = LocalTime.now();
                List<TimeWindowDto> windows = (today.getWindows() == null || today.getWindows().isEmpty())
                        ? List.of(new TimeWindowDto(today.getOpenAt(), today.getCloseAt()))
                        : today.getWindows();
                windows = windows.stream()
                        .filter(w -> w.getOpenAt() != null && w.getCloseAt() != null)
                        .sorted(Comparator.comparing(TimeWindowDto::getOpenAt))
                        .toList();
                if (!windows.isEmpty()) {
                    // Ouvert : fin de la plage en cours. Sinon : prochaine ouverture du jour
                    // (pause de midi comprise), à défaut la première ; fermeture = fin de journée.
                    TimeWindowDto current = windows.stream()
                            .filter(w -> !t.isBefore(w.getOpenAt()) && t.isBefore(w.getCloseAt()))
                            .findFirst().orElse(null);
                    openNow = current != null;
                    TimeWindowDto next = windows.stream()
                            .filter(w -> w.getOpenAt().isAfter(t)).findFirst().orElse(windows.get(0));
                    openAt = fmt(openNow ? current.getOpenAt() : next.getOpenAt());
                    closeAt = fmt(openNow ? current.getCloseAt() : windows.get(windows.size() - 1).getCloseAt());
                }
            }
        } catch (RuntimeException ignored) {
            // Horaires non configurés : le front affiche « horaires non renseignés ».
        }
        return new Extras(minPrice, categories, openNow, openAt, closeAt);
    }

    private static String fmt(LocalTime t) {
        return t == null ? null : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    @Override
    public List<SalonCatalogDto> listSalons() {
        return listSalons(null);
    }

    @Override
    public List<SalonCatalogDto> listSalons(Long localityId) {
        java.util.Set<Long> within = localityId == null ? null : localityService.salonIdsWithin(localityId);
        return salonRepository.findAll().stream()
                .filter(s -> within == null || within.contains(s.getId()))
                .map(s -> {
                    Extras x = extras(s.getId());
                    return SalonCatalogDto.builder()
                            .id(s.getId())
                            .name(s.getName())
                            .address(s.getAddress())
                            .phoneNumber(s.getPhoneNumber())
                            .localityId(s.getLocality() != null ? s.getLocality().getId() : null)
                            .localityPath(s.getLocality() != null ? localityService.pathOf(s.getLocality()) : null)
                            .latitude(s.getLatitude())
                            .longitude(s.getLongitude())
                            .establishmentTypeName(s.getType() != null ? s.getType().getName() : null)
                            .servicesCount(offeringRepository.countBySalon_IdAndActiveTrue(s.getId()))
                            .rating(ratingOf(s.getId()))
                            .reviewsCount(reviewRepository.countBySalon_Id(s.getId()))
                            .logoUrl(s.getLogoUrl())
                            .coverUrl(s.getCoverUrl())
                            .minPrice(x.minPrice())
                            .categories(x.categories())
                            .openNow(x.openNow())
                            .todayOpenAt(x.openAt())
                            .todayCloseAt(x.closeAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public SalonCatalogDetailDto getSalon(Long salonId) {
        Salon s = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
        Extras x = extras(salonId);
        return SalonCatalogDetailDto.builder()
                .id(s.getId())
                .name(s.getName())
                .address(s.getAddress())
                .phoneNumber(s.getPhoneNumber())
                .localityId(s.getLocality() != null ? s.getLocality().getId() : null)
                .localityPath(s.getLocality() != null ? localityService.pathOf(s.getLocality()) : null)
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .establishmentTypeName(s.getType() != null ? s.getType().getName() : null)
                .rating(ratingOf(salonId))
                .reviewsCount(reviewRepository.countBySalon_Id(salonId))
                .logoUrl(s.getLogoUrl())
                .coverUrl(s.getCoverUrl())
                .minPrice(x.minPrice())
                .categories(x.categories())
                .openNow(x.openNow())
                .todayOpenAt(x.openAt())
                .todayCloseAt(x.closeAt())
                .services(offeringService.list(salonId, true)) // services actifs
                .hours(hoursService.getWeeklyHours(salonId))
                .build();
    }

    @Override
    public List<SalonPublicReviewDto> latestReviews(Long salonId, int limit) {
        if (!salonRepository.existsById(salonId)) {
            throw new ResourceNotFoundException("Salon introuvable: " + salonId);
        }
        int max = Math.max(1, Math.min(limit, 50));
        return reviewRepository.findBySalon_IdOrderByCreatedAtDesc(salonId).stream()
                .limit(max)
                .map(CatalogServiceImpl::toPublic)
                .collect(Collectors.toList());
    }

    private static SalonPublicReviewDto toPublic(SalonReview r) {
        return SalonPublicReviewDto.builder()
                .id(r.getId())
                .rating(r.getOverallRating() == null ? 0 : r.getOverallRating())
                .comment(r.getComment())
                .tags(r.getTags())
                .clientName(publicName(r.getClient()))
                .serviceName(r.getSalonBooking() != null && r.getSalonBooking().getOffering() != null
                        ? r.getSalonBooking().getOffering().getName() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }

    /** « Salah K. » : jamais le nom complet ni un moyen de contact. */
    static String publicName(Client c) {
        if (c == null) return "Client";
        String first = c.getFirstName() == null ? "" : c.getFirstName().trim();
        String last = c.getLastName() == null ? "" : c.getLastName().trim();
        if (first.isEmpty() && last.isEmpty()) return "Client";
        if (first.isEmpty()) return last.substring(0, 1).toUpperCase() + ".";
        return last.isEmpty() ? first : first + " " + last.substring(0, 1).toUpperCase() + ".";
    }
}
