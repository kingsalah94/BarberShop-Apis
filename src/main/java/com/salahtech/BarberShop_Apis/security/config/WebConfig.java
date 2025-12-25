package com.salahtech.BarberShop_Apis.security.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Mappe l'URL http://localhost:8080/uploads/** vers le dossier physique
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/user/"); // Assurez-vous que ça matche votre FileStorageProperties
    }
}
