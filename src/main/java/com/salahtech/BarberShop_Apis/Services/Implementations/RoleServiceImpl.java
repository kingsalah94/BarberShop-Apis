package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.RoleDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;
import com.salahtech.BarberShop_Apis.reppsitories.PermissionRepository;
import com.salahtech.BarberShop_Apis.reppsitories.RoleRepository;
import com.salahtech.BarberShop_Apis.specs.RoleSpecifications;
import com.salahtech.BarberShop_Apis.Services.Interfaces.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<RoleDto> search(String q, Pageable pageable) {
        Specification<Role> spec = Specification.where(RoleSpecifications.nameLike(q));
        return roleRepo.findAll(spec, pageable).map(RoleDto::fromEntity);
    }

    @Override
    public RoleDto create(RoleDto dto) {
        if (roleRepo.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Role name already exists");
        }
        Role r = new Role(dto.getName(), dto.getDescription());
        if (dto.getPermissions() != null) {
            Set<Permission> perms = dto.getPermissions().stream()
                    .map(n -> permRepo.findByName(n)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + n)))
                    .collect(Collectors.toSet());
            r.setPermissions(perms);
        }
        return RoleDto.fromEntity(roleRepo.save(r));
    }

    @Override
    public RoleDto update(Long id, RoleDto dto) {
        Role r = roleRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (dto.getName() != null)
            r.setName(dto.getName());
        if (dto.getDescription() != null)
            r.setDescription(dto.getDescription());
        if (dto.getPermissions() != null) {
            Set<Permission> perms = dto.getPermissions().stream()
                    .map(n -> permRepo.findByName(n)
                            .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + n)))
                    .collect(Collectors.toSet());
            r.setPermissions(perms);
        }
        return RoleDto.fromEntity(roleRepo.save(r));
    }

    @Override
    public void delete(Long id) {
        if (!roleRepo.existsById(id))
            throw new ResourceNotFoundException("Role not found");
        roleRepo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDto findById(Long id) {
        return roleRepo.findById(id).map(RoleDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
    }
}
