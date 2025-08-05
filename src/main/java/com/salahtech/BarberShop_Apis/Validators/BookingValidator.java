package com.salahtech.BarberShop_Apis.Validators;

import java.util.ArrayList;
import java.util.List;

import com.salahtech.BarberShop_Apis.Dtos.BookingDto;

public class BookingValidator {

    public static List<String> validateBooking(BookingDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("La réservation ne peut pas être null");
            return errors;
        }
        if (dto.getServiceDto() == null || dto.getServiceDto().getId() == null) {
            errors.add("Le service est obligatoire dans une réservation");
        }
        if (dto.getUserDto() == null || dto.getUserDto().getId() == null) {
            errors.add("L'utilisateur est obligatoire dans une réservation");
        }
        if (dto.getBarberDto() == null || dto.getBarberDto().getId() == null) {
            errors.add("Le barbier est obligatoire dans une réservation");
        }

        return errors;
    }
}
