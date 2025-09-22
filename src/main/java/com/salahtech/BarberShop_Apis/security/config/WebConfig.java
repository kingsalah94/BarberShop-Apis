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
        // Permet de servir http://host:port/files/... depuis le dossier local
        String location = "file:" + (uploadDir.endsWith("/") ? uploadDir : uploadDir + "/");
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
}
