package com.salahtech.BarberShop_Apis.Validators;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;

public class ApplicationUserValidator {

    public static List<String> validateUser(ApplicationUserDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("L'utilisateur ne peut pas être null");
            return errors;
        }
        if (!StringUtils.hasLength(dto.getFirstName())) {
            errors.add("Le nom d'utilisateur est obligatoire");
        }
        if (!StringUtils.hasLength(dto.getEmail())) {
            errors.add("L'email est obligatoire");
        }
        if (!StringUtils.hasLength(dto.getPhone())) {
            errors.add("Le numero de telephone est obligatoire est obligatoire");
        }

        return errors;
    }
}
