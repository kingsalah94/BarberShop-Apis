package com.salahtech.BarberShop_Apis.Dtos;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UpdateProfileRequest {
    @Size(min = 1, max = 100)
    private String firstName;
    @Size(min = 1, max = 100)
    private String lastName;
    private String phone;
    private String profileImageUrl; // avatar_url côté entity
    private String preferences; // JSON/text libre
}
