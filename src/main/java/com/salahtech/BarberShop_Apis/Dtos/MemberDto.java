package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vue d'un sous-compte (membership) d'un salon. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
    private Long membershipId;
    private Long userId;
    private String name;
    private String email;
    private SalonRole role;
    private Long salonId;

    /** Renvoyé uniquement à la création d'un nouveau compte (à communiquer au membre). */
    private String tempPassword;
}
