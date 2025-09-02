package com.salahtech.BarberShop_Apis.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
public class RoleHierarchyConfig {

    @Bean
    RoleHierarchy roleHierarchy() {
        // Remplace new RoleHierarchyImpl() + setHierarchy(...)
        return RoleHierarchyImpl.fromHierarchy("""
                    ROLE_SUPER_ADMIN > ROLE_ADMIN
                    ROLE_ADMIN       > ROLE_MANAGER
                    ROLE_MANAGER     > ROLE_USER
                """);
    }
}
