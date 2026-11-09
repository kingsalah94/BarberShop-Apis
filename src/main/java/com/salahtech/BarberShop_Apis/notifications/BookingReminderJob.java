package com.salahtech.BarberShop_Apis.notifications;

import com.salahtech.BarberShop_Apis.domain.events.BookingReminderDueEvent;
import com.salahtech.BarberShop_Apis.repositories.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class BookingReminderJob {

    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher publisher;

    @Scheduled(cron = "0 0 9 * * *")
    public void scheduleReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        bookingRepository.findAllForDate(tomorrow)
                .forEach(b -> publisher.publishEvent(new BookingReminderDueEvent(b.getId())));
    }

}
