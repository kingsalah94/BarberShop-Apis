package com.salahtech.BarberShop_Apis.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private ProfileCompletenessInterceptor profileInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(profileInterceptor)
                .addPathPatterns("/v1/**")
                .excludePathPatterns("/v1/auth/**", "/v1/profile/complete");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir les fichiers uploadés comme ressources statiques
        registry.addResourceHandler("/api/v1/files/**")
                .addResourceLocations("file:uploads/");
    }
}
