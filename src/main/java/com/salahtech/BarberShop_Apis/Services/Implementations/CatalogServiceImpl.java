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
            // Plages de la veille (fin de nuit) + du jour : un salon ouvert 18:00 → 03:00 est « ouvert » à 01:00.
            LocalDate d = LocalDate.now();
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            List<com.salahtech.BarberShop_Apis.Utils.OpeningHours.Interval> ivs = new java.util.ArrayList<>();
            for (LocalDate day : List.of(d.minusDays(1), d)) {
                EffectiveHoursDto eff = hoursService.getEffectiveHours(salonId, day);
                if (eff == null || eff.isClosed()) continue;
                List<TimeWindowDto> windows = (eff.getWindows() == null || eff.getWindows().isEmpty())
                        ? List.of(new TimeWindowDto(eff.getOpenAt(), eff.getCloseAt()))
                        : eff.getWindows();
                ivs.addAll(com.salahtech.BarberShop_Apis.Utils.OpeningHours.intervals(day, windows));
            }
            ivs.sort(Comparator.comparing(com.salahtech.BarberShop_Apis.Utils.OpeningHours.Interval::start));
            var current = ivs.stream().filter(iv -> iv.containsInstant(now)).findFirst().orElse(null);
            openNow = current != null;
            if (openNow) {
                openAt = fmt(current.start().toLocalTime());
                closeAt = fmt(current.end().toLocalTime());
            } else {
                // Prochaine ouverture aujourd'hui ; fermeture = fin de la dernière plage du jour.
                var todays = ivs.stream().filter(iv -> iv.start().toLocalDate().equals(d)).toList();
                var next = todays.stream().filter(iv -> iv.start().isAfter(now)).findFirst()
                        .orElse(todays.isEmpty() ? null : todays.get(0));
                if (next != null) {
                    openAt = fmt(next.start().toLocalTime());
                    closeAt = fmt(todays.get(todays.size() - 1).end().toLocalTime());
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
