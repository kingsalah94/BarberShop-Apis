package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.CreateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Dtos.EmployeeDto;
import com.salahtech.BarberShop_Apis.Dtos.TimeOffDto;
import com.salahtech.BarberShop_Apis.Dtos.UpdateEmployeeRequest;
import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import com.salahtech.BarberShop_Apis.Exceptions.BusinessException;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.Enums.ApplicationUserType;
import com.salahtech.BarberShop_Apis.Enums.SalonRole;
import com.salahtech.BarberShop_Apis.Services.Interfaces.EmployeeService;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import com.salahtech.BarberShop_Apis.models.Barber;
import com.salahtech.BarberShop_Apis.models.Category;
import com.salahtech.BarberShop_Apis.models.Employee;
import com.salahtech.BarberShop_Apis.models.EmployeeTimeOff;
import com.salahtech.BarberShop_Apis.models.Salon;
import com.salahtech.BarberShop_Apis.models.SalonMembership;
import com.salahtech.BarberShop_Apis.repositories.ApplicationUserRepository;
import com.salahtech.BarberShop_Apis.repositories.BarberRepository;
import com.salahtech.BarberShop_Apis.repositories.CategoryRepository;
import com.salahtech.BarberShop_Apis.repositories.EmployeeRepository;
import com.salahtech.BarberShop_Apis.repositories.EmployeeTimeOffRepository;
import com.salahtech.BarberShop_Apis.repositories.RoleRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonMembershipRepository;
import com.salahtech.BarberShop_Apis.repositories.SalonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final SalonRepository salonRepository;
    private final ApplicationUserRepository userRepository;
    private final BarberRepository barberRepository;
    private final CategoryRepository categoryRepository;
    private final RoleRepository roleRepository;
    private final SalonMembershipRepository membershipRepository;
    private final EmployeeTimeOffRepository timeOffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public EmployeeDto create(CreateEmployeeRequest req) {
        if (req.getSalonIds() == null || !req.getSalonIds().contains(req.getPrimarySalonId())) {
            throw new BusinessException("Le salon principal doit faire partie des salons assignés");
        }

        Set<Salon> salons = loadSalons(req.getSalonIds());
        Salon primary = salons.stream()
                .filter(s -> s.getId().equals(req.getPrimarySalonId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Salon principal introuvable: " + req.getPrimarySalonId()));

        Employee employee = Employee.builder()
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .email(req.getEmail())
                .phoneNumber(req.getPhoneNumber())
                .role(req.getRole())
                .salons(salons)
                .primarySalon(primary)
                .calendarColor(req.getCalendarColor())
                .hireDate(req.getHireDate())
                .notes(req.getNotes())
                .isAvailable(true)
                .isActive(true)
                .skills(req.getSkillIds() != null
                        ? categoryRepository.findAllById(req.getSkillIds())
                        : List.of())
                .build();

        if (req.getUserId() != null) {
            employee.setUser(userRepository.findById(req.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Utilisateur introuvable: " + req.getUserId())));
        }

        if (req.getBarberId() != null) {
            if (req.getRole() != EmployeeRole.BARBER) {
                throw new BusinessException("Un barber ne peut être lié qu'à un employé de rôle BARBER");
            }
            Barber barber = barberRepository.findById(req.getBarberId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Barber introuvable: " + req.getBarberId()));
            employeeRepository.findByBarber_Id(req.getBarberId()).ifPresent(e -> {
                throw new BusinessException("Ce barber est déjà lié à un employé (id=" + e.getId() + ")");
            });
            employee.setBarber(barber);
        }

        Employee saved = employeeRepository.save(employee);

        // Provisioning optionnel d'un compte de connexion (espace pro du prestataire).
        String tempPassword = null;
        if (Boolean.TRUE.equals(req.getCreateLogin())) {
            tempPassword = provisionAccount(saved, primary, req.getLoginPassword());
            saved = employeeRepository.save(saved);
        }

        log.info("Employee créé id={} role={} salons={} login={}",
                saved.getId(), saved.getRole(), req.getSalonIds(), tempPassword != null);
        return toDto(saved, tempPassword);
    }

    @Override
    public EmployeeDto provisionLogin(Long id, String password) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        if (employee.getUser() != null) {
            throw new BusinessException("Ce prestataire a déjà un compte de connexion");
        }
        Salon primary = employee.getPrimarySalon();
        if (primary == null) {
            primary = employee.getSalons().stream().findFirst()
                    .orElseThrow(() -> new BusinessException("Aucun salon rattaché à ce prestataire"));
        }
        String tempPassword = provisionAccount(employee, primary, password);
        Employee saved = employeeRepository.save(employee);
        log.info("Login provisionné pour employee id={} (salon {})", saved.getId(), primary.getId());
        return toDto(saved, tempPassword);
    }

    /**
     * Crée un {@link ApplicationUser} (type EMPLOYEE, rôle Spring BARBER) + un
     * {@link SalonMembership} de rôle EMPLOYEE sur le salon principal, et relie
     * le compte à l'employé. Renvoie le mot de passe en clair (temporaire).
     */
    private String provisionAccount(Employee employee, Salon primary, String rawPassword) {
        final String email = employee.getEmail() != null ? employee.getEmail().trim().toLowerCase() : null;
        if (email == null || email.isBlank()) {
            throw new BusinessException("Un email est requis pour créer le compte de connexion du prestataire");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Un compte existe déjà avec cet email: " + email);
        }

        final String tempPassword = (rawPassword != null && !rawPassword.isBlank())
                ? rawPassword : generateTempPassword();

        ApplicationUser nu = new ApplicationUser();
        nu.setFirstName(employee.getFirstName());
        nu.setLastName(employee.getLastName());
        nu.setEmail(email);
        nu.setPhone(employee.getPhoneNumber());
        nu.setPassword(passwordEncoder.encode(tempPassword));
        nu.setMustChangePassword(false); // l'owner communique le mot de passe ; changeable ensuite
        nu.setUserType(ApplicationUserType.EMPLOYEE);
        // Rôle Spring : BARBER (déjà whitelisté pour les endpoints staff).
        roleRepository.findByName("BARBER").ifPresent(r -> nu.setRoles(Set.of(r)));
        nu.setIsVerified(true);
        nu.setIsEnabled(true);
        nu.setAccountNonLocked(true);
        nu.setCreatedAt(LocalDateTime.now());
        ApplicationUser user = userRepository.save(nu);

        if (!membershipRepository.existsByUser_IdAndSalon_Id(user.getId(), primary.getId())) {
            membershipRepository.save(SalonMembership.builder()
                    .user(user).salon(primary).role(SalonRole.EMPLOYEE).build());
        }
        employee.setUser(user);
        return tempPassword;
    }

    private String generateTempPassword() {
        // Respecte min 8 + maj/min/chiffre.
        return "Bs" + UUID.randomUUID().toString().substring(0, 6) + "9";
    }

    @Override
    public EmployeeDto update(Long id, UpdateEmployeeRequest req) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));

        if (req.getFirstName() != null) employee.setFirstName(req.getFirstName());
        if (req.getLastName() != null) employee.setLastName(req.getLastName());
        if (req.getEmail() != null) employee.setEmail(req.getEmail());
        if (req.getPhoneNumber() != null) employee.setPhoneNumber(req.getPhoneNumber());
        if (req.getRole() != null) employee.setRole(req.getRole());
        if (req.getCalendarColor() != null) employee.setCalendarColor(req.getCalendarColor());
        if (req.getHireDate() != null) employee.setHireDate(req.getHireDate());
        if (req.getNotes() != null) employee.setNotes(req.getNotes());
        if (req.getIsAvailable() != null) employee.setAvailable(req.getIsAvailable());
        if (req.getIsActive() != null) employee.setActive(req.getIsActive());

        if (req.getSalonIds() != null) {
            Set<Salon> salons = loadSalons(req.getSalonIds());
            employee.setSalons(salons);
            if (employee.getPrimarySalon() != null
                    && !req.getSalonIds().contains(employee.getPrimarySalon().getId())) {
                employee.setPrimarySalon(salons.iterator().next());
            }
        }

        if (req.getPrimarySalonId() != null) {
            Salon primary = employee.getSalons().stream()
                    .filter(s -> s.getId().equals(req.getPrimarySalonId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "Le salon principal doit faire partie des salons assignés"));
            employee.setPrimarySalon(primary);
        }

        if (req.getSkillIds() != null) {
            employee.setSkills(categoryRepository.findAllById(req.getSkillIds()));
        }

        return toDto(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto findById(Long id) {
        return toDto(employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findAllBySalon(Long salonId) {
        return employeeRepository.findAllBySalonId(salonId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findActiveBySalon(Long salonId) {
        return employeeRepository.findActiveBySalonId(salonId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findAvailableBySalon(Long salonId) {
        return employeeRepository.findAvailableBySalonId(salonId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeDto> findBySalonAndRole(Long salonId, EmployeeRole role) {
        return employeeRepository.findBySalonIdAndRole(salonId, role).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public EmployeeDto setAvailability(Long id, boolean isAvailable) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        employee.setAvailable(isAvailable);
        return toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto setActive(Long id, boolean isActive) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        employee.setActive(isActive);
        return toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto assignSalon(Long id, Long salonId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        Salon salon = salonRepository.findById(salonId)
                .orElseThrow(() -> new ResourceNotFoundException("Salon introuvable: " + salonId));
        employee.getSalons().add(salon);
        return toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto removeSalon(Long id, Long salonId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));

        if (employee.getPrimarySalon() != null && employee.getPrimarySalon().getId().equals(salonId)) {
            throw new BusinessException("Impossible de retirer le salon principal. Changez-le d'abord.");
        }
        employee.getSalons().removeIf(s -> s.getId().equals(salonId));
        return toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto addSkill(Long id, Long categoryId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable: " + categoryId));
        if (!employee.getSkills().contains(category)) {
            employee.getSkills().add(category);
        }
        return toDto(employeeRepository.save(employee));
    }

    @Override
    public EmployeeDto removeSkill(Long id, Long categoryId) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + id));
        employee.getSkills().removeIf(c -> c.getId().equals(categoryId));
        return toDto(employeeRepository.save(employee));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDto findByUserId(Long userId) {
        return toDto(employeeRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Aucun profil prestataire pour cet utilisateur")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeOffDto> listTimeOff(Long employeeId) {
        return timeOffRepository.findByEmployee_IdOrderByStartDateAsc(employeeId).stream()
                .map(this::toTimeOffDto).collect(Collectors.toList());
    }

    @Override
    public TimeOffDto addTimeOff(Long employeeId, TimeOffDto req) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employé introuvable: " + employeeId));
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new BusinessException("Dates de début et de fin requises");
        }
        if (req.getEndDate().isBefore(req.getStartDate())) {
            throw new BusinessException("La date de fin doit suivre la date de début");
        }
        EmployeeTimeOff saved = timeOffRepository.save(EmployeeTimeOff.builder()
                .employee(e)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason())
                .build());
        return toTimeOffDto(saved);
    }

    @Override
    public void deleteTimeOff(Long employeeId, Long timeOffId) {
        EmployeeTimeOff t = timeOffRepository.findById(timeOffId)
                .orElseThrow(() -> new ResourceNotFoundException("Congé introuvable: " + timeOffId));
        if (t.getEmployee() == null || !t.getEmployee().getId().equals(employeeId)) {
            throw new BusinessException("Ce congé n'appartient pas à ce prestataire");
        }
        timeOffRepository.delete(t);
    }

    private TimeOffDto toTimeOffDto(EmployeeTimeOff t) {
        return TimeOffDto.builder()
                .id(t.getId())
                .employeeId(t.getEmployee() != null ? t.getEmployee().getId() : null)
                .startDate(t.getStartDate())
                .endDate(t.getEndDate())
                .reason(t.getReason())
                .build();
    }

    @Override
    public void delete(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Employé introuvable: " + id);
        }
        employeeRepository.deleteById(id);
        log.info("Employee supprimé id={}", id);
    }

    private Set<Salon> loadSalons(Set<Long> salonIds) {
        Set<Salon> salons = new HashSet<>(salonRepository.findAllById(salonIds));
        if (salons.size() != salonIds.size()) {
            throw new ResourceNotFoundException("Un ou plusieurs salons sont introuvables");
        }
        return salons;
    }

    private EmployeeDto toDto(Employee e) {
        return toDto(e, null);
    }

    private EmployeeDto toDto(Employee e, String tempPassword) {
        return EmployeeDto.builder()
                .id(e.getId())
                .userId(e.getUser() != null ? e.getUser().getId() : null)
                .hasLogin(e.getUser() != null)
                .tempPassword(tempPassword)
                .barberId(e.getBarber() != null ? e.getBarber().getId() : null)
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .phoneNumber(e.getPhoneNumber())
                .role(e.getRole())
                .roleDisplayName(e.getRole() != null ? e.getRole().getDisplayName() : null)
                .salonIds(e.getSalons().stream().map(Salon::getId).collect(Collectors.toSet()))
                .primarySalonId(e.getPrimarySalon() != null ? e.getPrimarySalon().getId() : null)
                .isAvailable(e.isAvailable())
                .isActive(e.isActive())
                .calendarColor(e.getCalendarColor())
                .hireDate(e.getHireDate())
                .notes(e.getNotes())
                .skillIds(e.getSkills().stream().map(Category::getId).collect(Collectors.toList()))
                .build();
    }
}