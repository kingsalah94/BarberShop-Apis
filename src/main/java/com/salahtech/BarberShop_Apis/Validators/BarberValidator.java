package com.salahtech.BarberShop_Apis.Validators;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.salahtech.BarberShop_Apis.Dtos.BarberDto;

public class BarberValidator {

    public static List<String> validate(BarberDto dto) {
        List<String> errors = new ArrayList<>();

        if (dto == null) {
            errors.add("Le barbier ne peut pas être null");
            return errors;
        }
        if (!StringUtils.hasLength(dto.getSalonName())) {
            errors.add("Le prénom est obligatoire");
        }
       

        return errors;
    }
}
