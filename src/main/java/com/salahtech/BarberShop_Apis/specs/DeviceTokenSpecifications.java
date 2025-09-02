package com.salahtech.BarberShop_Apis.specs;

import com.salahtech.BarberShop_Apis.models.DeviceToken;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Set;

public final class DeviceTokenSpecifications {
    private DeviceTokenSpecifications() {
    }

    public static Specification<DeviceToken> byUserId(Long userId) {
        return (r, cq, cb) -> userId == null ? cb.conjunction() : cb.equal(r.get("userId"), userId);
    }

    public static Specification<DeviceToken> active(Boolean active) {
        return (r, cq, cb) -> active == null ? cb.conjunction() : cb.equal(r.get("active"), active);
    }

    public static Specification<DeviceToken> deviceTypesIn(Set<String> types) {
        return (r, cq, cb) -> (types == null || types.isEmpty())
                ? cb.conjunction()
                : r.get("deviceType").in(types);
    }

    public static Specification<DeviceToken> updatedBefore(LocalDateTime ts) {
        return (r, cq, cb) -> ts == null ? cb.conjunction() : cb.lessThan(r.get("updatedAt"), ts);
    }
}
