package com.salahtech.BarberShop_Apis.Validators;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

import org.springframework.util.StringUtils;
import com.salahtech.BarberShop_Apis.Dtos.ServiceDto;

public class ServiceValidator {

    public static List<String> validateService(ServiceDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Le service ne peut pas être null");
            return errors;
        }

        if (!StringUtils.hasLength(dto.getName())) {
            errors.add("Le nom du service est obligatoire");
        }

        // ✅ CORRECTION PRIX (Double vs BigDecimal)
        if (dto.getPrice() == null || dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Le prix du service doit être positif");
        }

        if (dto.getDuration() == null || dto.getDuration() <= 0) {
            errors.add("La durée du service est obligatoire et doit être positive");
        }

        // ✅ MODIFICATION MAJEURE ICI
        // On ne vérifie plus l'Enum (getCategory), mais l'ID (getCategoryId)
        if (dto.getCategoryId() == null) {
            errors.add("La catégorie du service est obligatoire (Veuillez sélectionner une catégorie)");
        }

        return errors;
    }
}