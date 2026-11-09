package com.salahtech.BarberShop_Apis.queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.salahtech.BarberShop_Apis.Enums.BookingStatus;
import com.salahtech.BarberShop_Apis.Enums.PaymentMethod;
import com.salahtech.BarberShop_Apis.Enums.PaymentStatus;
import com.salahtech.BarberShop_Apis.Enums.QueueStatus;
import com.salahtech.BarberShop_Apis.Enums.StationStatus;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Services.Implementations.QueueNotifier;
import com.salahtech.BarberShop_Apis.Services.Implementations.QueueServiceImpl;
import com.salahtech.BarberShop_Apis.models.QueueEntry;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonBooking;
import com.salahtech.BarberShop_Apis.models.Workstation;
import com.salahtech.BarberShop_Apis.repositories.BarberServiceRepository;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import com.salahtech.BarberShop_Apis.repositories.ClientRepository;
import com.salahtech.BarberShop_Apis.repositories.EmployeeRepository;
import com.salahtech.BarberShop_Apis.repositories.QueueEntryRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonBookingRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import com.salahtech.BarberShop_Apis.repositories.WorkstationRepository;

/** Cycle de vie d'une entrée de file : encaissement, fin, annulation, absence. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QueueServiceImplTest {

    @Mock QueueEntryRepository queueRepository;
    @Mock SalonRepository salonRepository;
    @Mock ClientRepository clientRepository;
    @Mock EmployeeRepository employeeRepository;
    @Mock WorkstationRepository workstationRepository;
    @Mock BarberServiceRepository barberServiceRepository;
    @Mock BookingRepository bookingRepository;
    @Mock SalonBookingRepository salonBookingRepository;
    @Mock QueueNotifier notifier;

    @InjectMocks QueueServiceImpl service;

    private Salon salon;

    @BeforeEach
    void setUp() {
        salon = new Salon();
        salon.setId(1L);
        when(queueRepository.save(any(QueueEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private QueueEntry entry(QueueStatus status) {
        QueueEntry e = QueueEntry.builder()
                .salon(salon)
                .status(status)
                .checkInTime(LocalDateTime.now())
                .customerName("Test")
                .build();
        e.setId(10L);
        e.setPaymentStatus(PaymentStatus.PENDING);
        when(queueRepository.findById(10L)).thenReturn(Optional.of(e));
        return e;
    }

    // ---------- Encaissement ----------

    @Test
    void pay_prestationEnCours_encaisse() {
        QueueEntry e = entry(QueueStatus.IN_CHAIR);

        var dto = service.pay(10L, PaymentMethod.CREDIT_CARD, new BigDecimal("25.00"));

        assertThat(dto.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(e.getAmount()).isEqualByComparingTo("25.00");
        assertThat(e.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
    }

    @Test
    void pay_deuxFois_refuse() {
        QueueEntry e = entry(QueueStatus.COMPLETED);
        e.setPaymentStatus(PaymentStatus.PAID);

        assertThatThrownBy(() -> service.pay(10L, PaymentMethod.CASH, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà encaissée");
        verify(queueRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"WAITING", "NOTIFIED", "CANCELLED", "NO_SHOW"})
    void pay_horsPrestation_refuse(QueueStatus status) {
        entry(status);

        assertThatThrownBy(() -> service.pay(10L, PaymentMethod.CASH, BigDecimal.TEN))
                .isInstanceOf(BusinessException.class);
        verify(queueRepository, never()).save(any());
    }

    @Test
    void pay_sansPrix_refuseZero() {
        entry(QueueStatus.COMPLETED);

        assertThatThrownBy(() -> service.pay(10L, PaymentMethod.CASH, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("impossible d'encaisser 0");
    }

    // ---------- Fin de prestation ----------

    @Test
    void complete_passeLaReservationSalonACompleted_etLePosteEnNettoyage() {
        QueueEntry e = entry(QueueStatus.IN_CHAIR);
        Workstation ws = Workstation.builder().status(StationStatus.OCCUPIED).build();
        e.setWorkstation(ws);
        e.setSalonBookingId(99L);
        SalonBooking sb = new SalonBooking();
        sb.setStatus(BookingStatus.CONFIRMED);
        when(salonBookingRepository.findById(99L)).thenReturn(Optional.of(sb));

        service.complete(10L);

        assertThat(e.getStatus()).isEqualTo(QueueStatus.COMPLETED);
        assertThat(sb.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(ws.getStatus()).isEqualTo(StationStatus.CLEANING);
    }

    @Test
    void complete_reservationAnnulee_resteAnnulee() {
        QueueEntry e = entry(QueueStatus.IN_CHAIR);
        e.setSalonBookingId(99L);
        SalonBooking sb = new SalonBooking();
        sb.setStatus(BookingStatus.CANCELLED);
        when(salonBookingRepository.findById(99L)).thenReturn(Optional.of(sb));

        service.complete(10L);

        assertThat(sb.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(salonBookingRepository, never()).save(any());
    }

    @Test
    void complete_estIdempotent() {
        entry(QueueStatus.COMPLETED);

        var dto = service.complete(10L);

        assertThat(dto.getStatus()).isEqualTo(QueueStatus.COMPLETED);
        verify(queueRepository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"CANCELLED", "NO_SHOW"})
    void complete_entreeCloturee_refuse(QueueStatus status) {
        entry(status);

        assertThatThrownBy(() -> service.complete(10L)).isInstanceOf(BusinessException.class);
    }

    // ---------- Annulation / absence ----------

    @Test
    void cancel_enCours_libereLePoste() {
        QueueEntry e = entry(QueueStatus.IN_CHAIR);
        Workstation ws = Workstation.builder().status(StationStatus.OCCUPIED).build();
        e.setWorkstation(ws);

        service.cancel(10L, "client parti");

        assertThat(e.getStatus()).isEqualTo(QueueStatus.CANCELLED);
        assertThat(ws.getStatus()).isEqualTo(StationStatus.FREE);
        assertThat(e.getNotes()).contains("Annulé: client parti");
    }

    @Test
    void cancel_estIdempotent() {
        entry(QueueStatus.CANCELLED);

        service.cancel(10L, null);

        verify(queueRepository, never()).save(any());
    }

    @Test
    void cancel_prestationTerminee_refuse() {
        entry(QueueStatus.COMPLETED);

        assertThatThrownBy(() -> service.cancel(10L, null)).isInstanceOf(BusinessException.class);
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"WAITING", "NOTIFIED"})
    void markNoShow_clientEnAttente_ok(QueueStatus status) {
        QueueEntry e = entry(status);

        service.markNoShow(10L);

        assertThat(e.getStatus()).isEqualTo(QueueStatus.NO_SHOW);
    }

    @ParameterizedTest
    @EnumSource(value = QueueStatus.class, names = {"IN_CHAIR", "COMPLETED", "CANCELLED"})
    void markNoShow_horsAttente_refuse(QueueStatus status) {
        entry(status);

        assertThatThrownBy(() -> service.markNoShow(10L)).isInstanceOf(BusinessException.class);
    }
}
