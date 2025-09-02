package com.salahtech.BarberShop_Apis.specs;

import com.salahtech.BarberShop_Apis.models.ApplicationUser;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public final class UserSpecifications {
    private UserSpecifications() {
    }

    public static Specification<ApplicationUser> emailLike(String q) {
        return (root, cq, cb) -> q == null || q.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("email")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<ApplicationUser> nameLike(String q) {
        return (root, cq, cb) -> q == null || q.isBlank()
                ? cb.conjunction()
                : cb.or(
                        cb.like(cb.lower(root.get("firstName")), "%" + q.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("lastName")), "%" + q.toLowerCase() + "%"));
    }

    public static Specification<ApplicationUser> enabledEq(Boolean enabled) {
        return (root, cq, cb) -> enabled == null ? cb.conjunction()
                : cb.equal(root.get("isEnabled"), enabled);
    }

    public static Specification<ApplicationUser> verifiedEq(Boolean verified) {
        return (root, cq, cb) -> verified == null ? cb.conjunction()
                : cb.equal(root.get("isVerified"), verified);
    }

    public static Specification<ApplicationUser> hasRole(String roleName) {
        return (root, cq, cb) -> {
            if (roleName == null || roleName.isBlank())
                return cb.conjunction();
            var roles = root.join("roles", JoinType.LEFT);
            return cb.equal(cb.lower(roles.get("name")), roleName.toLowerCase());
        };
    }

    public static Specification<ApplicationUser> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, cq, cb) -> {
            if (from == null && to == null)
                return cb.conjunction();
            if (from != null && to != null)
                return cb.between(root.get("createdAt"), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }
}
