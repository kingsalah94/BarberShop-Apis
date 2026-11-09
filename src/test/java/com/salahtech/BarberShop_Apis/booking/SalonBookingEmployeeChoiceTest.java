package com.salahtech.BarberShop_Apis.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.salahtech.BarberShop_Apis.Dtos.CreateSalonBookingRequest;
import com.salahtech.BarberShop_Apis.Dtos.EffectiveHoursDto;
import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Services.Implementations.SalonBookingServiceImpl;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonHoursService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Client;
import com.salahtech.BarberShop_Apis.models.Employee;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonBooking;
import com.salahtech.BarberShop_Apis.models.SalonOffering;
import com.salahtech.BarberShop_Apis.models.Workstation;
import com.salahtech.BarberShop_Apis.repositories.EmployeeRepository;
import com.salahtech.BarberShop_Apis.repositories.EmployeeTimeOffRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonOfferingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.WorkstationRepository;

/** Réservation salon avec choix du prestataire par le client. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SalonBookingEmployeeChoiceTest {

    @Mock SalonBookingRepository bookingRepository;
    @Mock SalonOfferingRepository offeringRepository;
    @Mock SalonRepository salonRepository;
    @Mock WorkstationRepository workstationRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock EmployeeTimeOffRepository timeOffRepository;
    @Mock SalonHoursService hoursService;

    @InjectMocks SalonBookingServiceImpl service;

    private final LocalDate day = LocalDate.now().plusDays(1);
    private Salon salon;
    private Employee alice;
    private Employee bob;
    private final List<SalonBooking> dayBookings = new ArrayList<>();

    @BeforeEach
    void setUp() {
        salon = new Salon();
        salon.setId(1L);
        when(salonRepository.findById(1L)).thenReturn(Optional.of(salon));

        SalonOffering coupe = SalonOffering.builder().id(5L).salon(salon).name("Coupe")
                .durationMinutes(30).price(new BigDecimal("5000")).build();
        when(offeringRepository.findById(5L)).thenReturn(Optional.of(coupe));

        // Ouvert 09:00-10:00 → créneaux 09:00, 09:15, 09:30 ; 3 postes.
        when(hoursService.getEffectiveHours(1L, day)).thenReturn(EffectiveHoursDto.builder()
                .date(day).closed(false).openAt(LocalTime.of(9, 0)).closeAt(LocalTime.of(10, 0)).build());
        when(workstationRepository.findBySalon_IdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(new Workstation(), new Workstation(), new Workstation()));
        when(bookingRepository.findBySalon_IdAndStatusInAndStartAtBetween(eq(1L), any(), any(), any()))
                .thenReturn(dayBookings);

        alice = employee(10L, "Alice", "Martin");
        bob = employee(11L, "Bob", "Diallo");
    }

    private Employee employee(Long id, String first, String last) {
        Employee e = Employee.builder().id(id).firstName(first).lastName(last).role(EmployeeRole.BARBER)
                .salons(Set.of(salon)).build();
        e.setActive(true);
        when(employeeRepository.findById(id)).thenReturn(Optional.of(e));
        return e;
    }

    private void booked(Employee e, int h, int m) {
        SalonBooking b = new SalonBooking();
        b.setStartAt(day.atTime(h, m));
        b.setEndAt(day.atTime(h, m).plusMinutes(30));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setAssignedEmployee(e);
        dayBookings.add(b);
    }

    @Test
    void peuImporte_seuleLaCapaciteDuSalonCompte() {
        booked(alice, 9, 0);

        assertThat(service.availableSlots(1L, 5L, day, null)).containsExactly("09:00", "09:15", "09:30");
    }

    @Test
    void prestataireChoisi_sesCreneauxOccupesDisparaissent() {
        booked(alice, 9, 0); // 09:00-09:30

        assertThat(service.availableSlots(1L, 5L, day, 10L)).containsExactly("09:30");
        assertThat(service.availableSlots(1L, 5L, day, 11L)).containsExactly("09:00", "09:15", "09:30");
    }

    @Test
    void prestataireEnConge_aucunCreneau() {
        when(timeOffRepository.existsByEmployee_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(10L, day, day))
                .thenReturn(true);

        assertThat(service.availableSlots(1L, 5L, day, 10L)).isEmpty();
    }

    @Test
    void prestataireInactif_aucunCreneau() {
        alice.setActive(false);

        assertThat(service.availableSlots(1L, 5L, day, 10L)).isEmpty();
    }

    @Test
    void prestataireDUnAutreSalon_refuse() {
        Salon autre = new Salon();
        autre.setId(2L);
        Employee intrus = Employee.builder().id(20L).firstName("X").salons(Set.of(autre)).build();
        when(employeeRepository.findById(20L)).thenReturn(Optional.of(intrus));

        assertThatThrownBy(() -> service.availableSlots(1L, 5L, day, 20L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void creation_avecPrestataireLibre_ilEstAffecte() {
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.getClient()).thenReturn(new Client());
        when(bookingRepository.save(any(SalonBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(1L, CreateSalonBookingRequest.builder()
                .serviceId(5L).date(day).time("09:00").employeeId(11L).build(), user);

        ArgumentCaptor<SalonBooking> saved = ArgumentCaptor.forClass(SalonBooking.class);
        org.mockito.Mockito.verify(bookingRepository).save(saved.capture());
        assertThat(saved.getValue().getAssignedEmployee()).isSameAs(bob);
    }

    @Test
    void creation_avecPrestataireDejaPris_refusee() {
        booked(alice, 9, 0);
        ApplicationUser user = mock(ApplicationUser.class);
        when(user.getClient()).thenReturn(new Client());

        assertThatThrownBy(() -> service.create(1L, CreateSalonBookingRequest.builder()
                .serviceId(5L).date(day).time("09:15").employeeId(10L).build(), user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("plus disponible");
    }

    @Test
    void listePublique_sansDonneesDeContact() {
        alice.setEmail("alice@salon.fr");
        alice.setPhoneNumber("+22700000000");
        when(employeeRepository.findBySalonIdAndRole(1L, EmployeeRole.BARBER)).thenReturn(List.of(alice));

        var staff = service.bookableStaff(1L);

        assertThat(staff).singleElement().satisfies(s -> {
            assertThat(s.getId()).isEqualTo(10L);
            assertThat(s.getDisplayName()).isEqualTo("Alice M.");
        });
        assertThat(staff.toString()).doesNotContain("alice@salon.fr", "+22700000000");
    }
}
