package com.salahtech.BarberShop_Apis.Services.Implementations;

import com.salahtech.BarberShop_Apis.Dtos.PermissionDto;
import com.salahtech.BarberShop_Apis.Exceptions.ResourceNotFoundException;
import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.reppsitories.PermissionRepository;
import com.salahtech.BarberShop_Apis.specs.PermissionSpecifications;
import com.salahtech.BarberShop_Apis.Services.Interfaces.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository repo;

    @Override
    @Transactional(readOnly = true)
    public Page<PermissionDto> search(String q, Pageable pageable) {
        Specification<Permission> spec = Specification.where(PermissionSpecifications.nameLike(q));
        return repo.findAll(spec, pageable).map(PermissionDto::fromEntity);
    }

    @Override
    @Transactional
    public PermissionDto create(PermissionDto dto) {
        if (repo.existsByName(dto.getName())) {
            throw new IllegalArgumentException("Permission name already exists");
        }
        Permission p = new Permission(dto.getName(), dto.getDescription());
        return PermissionDto.fromEntity(repo.save(p));
    }

    @Override
    @Transactional
    public PermissionDto update(Long id, PermissionDto dto) {
        Permission p = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
        if (dto.getName() != null)
            p.setName(dto.getName());
        if (dto.getDescription() != null)
            p.setDescription(dto.getDescription());
        return PermissionDto.fromEntity(repo.save(p));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id))
            throw new ResourceNotFoundException("Permission not found");
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionDto findById(Long id) {
        return repo.findById(id).map(PermissionDto::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found"));
    }
}
