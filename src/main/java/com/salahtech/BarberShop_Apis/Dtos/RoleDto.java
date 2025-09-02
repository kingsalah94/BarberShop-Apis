package com.salahtech.BarberShop_Apis.Dtos;

import com.salahtech.BarberShop_Apis.models.Permission;
import com.salahtech.BarberShop_Apis.models.Role;
import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private Long id;
    private String name;
    private String description;
    private Set<String> permissions;

    public static RoleDto fromEntity(Role r) {
        if (r == null)
            return null;
        return RoleDto.builder()
                .id(r.getId())
                .name(r.getName())
                .description(r.getDescription())
                .permissions(r.getPermissions() != null
                        ? r.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet())
                        : null)
                .build();
    }
}
