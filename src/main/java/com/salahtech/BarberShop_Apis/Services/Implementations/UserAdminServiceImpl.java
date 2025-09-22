package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.ApplicationUserDto;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Services.EmailService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.UserAdminService;
import com.salahtech.BarberShop_Apis.Utils.PasswordGenerator;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;
import com.salahtech.BarberShop_Apis.specs.UserSpecifications;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.*;
import org.springframework.util.StringUtils;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserAdminServiceImpl implements UserAdminService {

    private final ApplicationUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    @Override
    public Page<ApplicationUserDto> search(
            String q,
            Boolean enabled,
            Boolean verified,
            String role,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Pageable pageable) {
        // Base: toujours vrai, distinct pour éviter les doublons avec le join sur roles
        Specification<ApplicationUser> spec = (root, query, cb) -> {
            query.distinct(true);
            return cb.conjunction();
        };

        // Recherche plein texte : email, prénom, nom, phone + id exact si chiffre
        if (StringUtils.hasText(q)) {
            final var tokens = q.trim().split("\\s+");
            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                List<Predicate> allTokens = new ArrayList<>();
                for (String t : tokens) {
                    String like = "%" + t.toLowerCase() + "%";
                    Predicate tokenPred = cb.or(
                            cb.like(cb.lower(root.get("email")), like),
                            cb.like(cb.lower(root.get("firstName")), like),
                            cb.like(cb.lower(root.get("lastName")), like),
                            cb.like(cb.lower(root.get("phone")), like));
                    // si le token est numérique: match id exact
                    if (t.chars().allMatch(Character::isDigit)) {
                        tokenPred = cb.or(tokenPred, cb.equal(root.get("id"), Long.valueOf(t)));
                    }
                    allTokens.add(tokenPred);
                }
                // Tous les tokens doivent matcher (ET entre tokens, OU entre champs)
                return cb.and(allTokens.toArray(new Predicate[0]));
            });
        }

        // Filtre enabled
        if (enabled != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isEnabled"), enabled));
        }

        // Filtre verified
        if (verified != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("isVerified"), verified));
        }

        // Filtre rôle (LEFT JOIN) — accepte "ADMIN" ou "ROLE_ADMIN"
        if (StringUtils.hasText(role)) {
            final String normalized = role.toUpperCase().startsWith("ROLE_")
                    ? role.substring(5).toUpperCase()
                    : role.toUpperCase();

            spec = spec.and((root, query, cb) -> {
                query.distinct(true);
                Join<ApplicationUser, Role> r = root.join("roles", JoinType.LEFT);
                return cb.equal(cb.upper(r.get("name")), normalized);
            });
        }

        // Filtre date de création (bornes inclusives)
        if (createdFrom != null || createdTo != null) {
            final LocalDateTime from = createdFrom != null ? createdFrom : LocalDateTime.MIN;
            final LocalDateTime to = createdTo != null ? createdTo : LocalDateTime.MAX;
            spec = spec.and((root, query, cb) -> cb.between(root.get("createdAt"), from, to));
        }

        // Exécution + mapping
        return userRepo.findAll(spec, pageable).map(ApplicationUserDto::fromEntity);
    }

    @Override
    @Transactional
    public ApplicationUserDto create(ApplicationUserDto dto, String rawPassword) {
        // 0) Générer un mot de passe si vide
        String effectivePassword = (rawPassword == null || rawPassword.isBlank())
                ? PasswordGenerator.generate(12) // => util ci-dessous
                : rawPassword;

        ApplicationUser user = new ApplicationUser();
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(effectivePassword));
        user.setCreatedAt(LocalDateTime.now());
        user.setProvider(AuthProvider.LOCAL);

        ApplicationUserType userType = dto.getUserType() != null ? dto.getUserType() : ApplicationUserType.CLIENT;
        user.setUserType(userType);

        boolean enabled = Boolean.TRUE.equals(dto.getIsEnabled());
        boolean verified = Boolean.TRUE.equals(dto.getIsVerified());
        user.setIsEnabled(enabled);
        user.setIsVerified(verified);

        if (!verified) {
            user.setVerificationToken(java.util.UUID.randomUUID().toString());
        }

        // Rôles (inchangé)
        Set<Role> roles;
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            roles = dto.getRoles().stream()
                    .map(name -> roleRepo.findByName(name)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + name)))
                    .collect(java.util.stream.Collectors.toSet());
        } else {
            String roleName = switch (userType) {
                case CLIENT -> "CLIENT";
                case BARBER -> "BARBER";
                case SALON_OWNER -> "SALON_OWNER";
                case ADMIN -> "ADMIN";
            };
            Role role = roleRepo.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle " + roleName + " non trouvé"));
            roles = Set.of(role);
        }
        user.setRoles(roles);

        // Persist
        user = userRepo.save(user);

        // Emails : identifiants + (si non vérifié) lien de vérification
        try {
            // Envoi des identifiants avec le mot de passe en clair
            emailService.sendAccountCredentialsEmail(user, effectivePassword, verified);

            // Si flux de vérification email, on envoie aussi l'email de vérif (comme avant)
            if (!verified) {
                emailService.sendVerificationEmail(user);
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger(getClass())
                    .warn("Création OK mais échec d’envoi des emails pour {}: {}", user.getEmail(), ex.getMessage());
        }

        return ApplicationUserDto.fromEntity(user);
    }

    @Override
    public ApplicationUserDto update(Long id, ApplicationUserDto dto) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update champs simples via helper du DTO si tu l’as
        ApplicationUserDto.updateEntity(u, dto);

        // UserType (protéger la contrainte NOT NULL)
        if (dto.getUserType() != null) {
            u.setUserType(dto.getUserType());
        } else if (u.getUserType() == null) {
            u.setUserType(ApplicationUserType.CLIENT);
        }

        // Rôles
        if (dto.getRoles() != null) {
            var roles = dto.getRoles().stream()
                    .map(n -> roleRepo.findByName(n)
                            .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)))
                    .collect(Collectors.toSet());
            u.setRoles(roles);
        }

        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public void delete(Long id) {
        if (!userRepo.existsById(id))
            throw new ResourceNotFoundException("User not found");
        userRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationUserDto findById(Long id) {
        return userRepo.findById(id)
                .map(ApplicationUserDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Override
    public ApplicationUserDto setEnabled(Long id, boolean value) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setIsEnabled(value);
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public ApplicationUserDto setVerified(Long id, boolean value) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setIsVerified(value);
        if (value) {
            // Si on force la vérification, on purge le token
            u.setVerificationToken(null);
        } else if (u.getVerificationToken() == null) {
            // On (re)génère un token si on repasse à non vérifié
            u.setVerificationToken(UUID.randomUUID().toString());
        }
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }

    @Override
    public ApplicationUserDto setRoles(Long id, Set<String> roleNames) {
        var u = userRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var roles = roleNames.stream()
                .map(n -> roleRepo.findByName(n)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + n)))
                .collect(Collectors.toSet());
        u.setRoles(roles);
        return ApplicationUserDto.fromEntity(userRepo.save(u));
    }
}
