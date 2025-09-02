package com.salahtech.BarberShop_Apis.specs;

import com.salahtech.BarberShop_Apis.models.Permission;
import org.springframework.data.jpa.domain.Specification;

public final class PermissionSpecifications {
    private PermissionSpecifications() {
    }

    public static Specification<Permission> nameLike(String q) {
        return (root, cq, cb) -> (q == null || q.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%");
    }
}
