package com.salahtech.BarberShop_Apis.Dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplyRequestDto {
    @NotBlank
    private String reply;
}