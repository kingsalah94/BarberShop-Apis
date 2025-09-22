package com.salahtech.BarberShop_Apis.Services;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${app.upload.dir:uploads}")
    private String rootDir;

    public String saveImage(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED.contains(ct)) {
            throw new IllegalArgumentException("Type non supporté (jpeg, png, webp uniquement)");
        }

        String ext = switch (ct) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };

        Path base = Paths.get(rootDir).toAbsolutePath().normalize();
        if (folder != null && !folder.isBlank())
            base = base.resolve(folder);
        Files.createDirectories(base);

        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dest = base.resolve(name);

        // écriture atomique
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

        // Chemin web relatif à /files/**
        String rel = (folder == null || folder.isBlank())
                ? name
                : folder.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "") + "/" + name;

        return rel; // ex: "services/abcd1234.jpg"
    }
}
