package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Dtos.CreateMemberRequest;
import com.salahtech.BarberShop_Apis.Dtos.MemberDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.Interfaces.SalonMembershipService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonMembership;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonMembershipRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SalonMembershipServiceImpl implements SalonMembershipService {

    private final SalonMembershipRepository membershipRepository;
    private final ApplicationUserRepository userRepository;
    private final SalonRepository salonRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantGuard tenantGuard;

    @Override
    @Transactional(readOnly = true)
    public List<MemberDto> listMembers(ApplicationUser current, Long salonId) {
        // Voir les comptes : OWNER + MANAGER (gestion du personnel).
        tenantGuard.assertCanManageStaff(current, salonId);
        return membershipRepository.findBySalon_Id(salonId).stream()
                .map(m -> toDto(m, null))
                .toList();
    }

    @Override
    public MemberDto createMember(ApplicationUser current, Long salonId, CreateMemberRequest req) {
        // Créer un sous-compte : OWNER uniquement.
        tenantGuard.assertCanManageMembers(current, salonId);
        if (req.getRole() == SalonRole.OWNER) {
            throw new BusinessException("Impossible de créer un second propriétaire via cet écran");
        }
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));

        final String email = req.getEmail().trim().toLowerCase();
        String tempPassword = null;

        ApplicationUser user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            tempPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                    ? req.getPassword()
                    : generateTempPassword();
            final ApplicationUser nu = new ApplicationUser();
            nu.setFirstName(req.getFirstName());
            nu.setLastName(req.getLastName());
            nu.setEmail(email);
            nu.setPhone(req.getPhone());
            nu.setPassword(passwordEncoder.encode(tempPassword));
            nu.setMustChangePassword(true);
            nu.setUserType(req.getRole() == SalonRole.MANAGER
                    ? ApplicationUserType.MANAGER : ApplicationUserType.RECEPTIONIST);
            nu.setVerificationToken(UUID.randomUUID().toString());
            roleRepository.findByName(req.getRole().name()).ifPresent(r -> nu.setRoles(Set.of(r)));
            nu.setIsVerified(true);
            nu.setIsEnabled(true);
            nu.setAccountNonLocked(true);
            nu.setCreatedAt(LocalDateTime.now());
            user = userRepository.save(nu);
        }

        if (membershipRepository.existsByUser_IdAndSalon_Id(user.getId(), salonId)) {
            throw new BusinessException("Cet utilisateur est déjà membre de ce salon");
        }

        SalonMembership saved = membershipRepository.save(SalonMembership.builder()
                .user(user).salon(salon).role(req.getRole()).build());
        log.info("Sous-compte {} créé pour salon {} (role={})", email, salonId, req.getRole());
        return toDto(saved, tempPassword);
    }

    @Override
    public void revokeMember(ApplicationUser current, Long salonId, Long membershipId) {
        tenantGuard.assertCanManageMembers(current, salonId);
        SalonMembership m = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable: " + membershipId));
        if (m.getSalon() == null || !m.getSalon().getId().equals(salonId)) {
            throw new AccessDeniedException("Ce membre n'appartient pas à ce salon");
        }
        if (m.getRole() == SalonRole.OWNER) {
            throw new BusinessException("Impossible de retirer le propriétaire du salon");
        }
        membershipRepository.delete(m);
        log.info("Sous-compte membership {} retiré du salon {}", membershipId, salonId);
    }

    private String generateTempPassword() {
        // Respecte min 8 + maj/min/chiffre.
        return "Bs" + UUID.randomUUID().toString().substring(0, 6) + "9";
    }

    private MemberDto toDto(SalonMembership m, String tempPassword) {
        ApplicationUser u = m.getUser();
        String name = u == null ? null
                : ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                   + (u.getLastName() != null ? u.getLastName() : "")).trim();
        return MemberDto.builder()
                .membershipId(m.getId())
                .userId(u != null ? u.getId() : null)
                .name(name)
                .email(u != null ? u.getEmail() : null)
                .role(m.getRole())
                .salonId(m.getSalon() != null ? m.getSalon().getId() : null)
                .tempPassword(tempPassword)
                .build();
    }
}
