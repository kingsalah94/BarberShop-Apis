package com.salahtech.BarberShop_Apis.Services.Interfaces;

import com.salahtech.BarberShop_Apis.Dtos.CreateMemberRequest;
import com.salahtech.BarberShop_Apis.Dtos.MemberDto;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import java.util.List;

/** Gestion des sous-comptes d'un salon (Owner / Manager / Réceptionniste). */
public interface SalonMembershipService {

    List<MemberDto> listMembers(ApplicationUser current, Long salonId);

    MemberDto createMember(ApplicationUser current, Long salonId, CreateMemberRequest request);

    void revokeMember(ApplicationUser current, Long salonId, Long membershipId);
}
