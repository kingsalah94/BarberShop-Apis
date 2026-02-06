package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.salahtech.BarberShop_Apis.Config.FileStorageProperties;
import com.salahtech.BarberShop_Apis.Exceptions.FileUploadException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Service de gestion du stockage des fichiers
 */
@Service
@Slf4j
public class FileStorageService {

    private Path fileStorageLocation;

    @Autowired
    public void FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
                .toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new FileUploadException("Impossible de créer le dossier d'upload.", ex);
        }
    }

    public String storeFile(MultipartFile file) {
        // 1. Nettoyer le nom du fichier
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

        // 2. Générer un nom unique (UUID) pour éviter les conflits
        // Ex: "avatar.jpg" devient "a1b2c3d4-avatar.jpg"
        String fileExtension = "";
        try {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        } catch (Exception e) {
            fileExtension = "";
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // Vérification de sécurité (pas de ".." dans le chemin)
            if (fileName.contains("..")) {
                throw new FileUploadException("Nom de fichier invalide " + fileName);
            }

            // 3. Copier le fichier
            Path targetLocation = this.fileStorageLocation.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 4. Retourner l'URL complète accessible
            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/") // Doit correspondre à la config WebMvc (voir étape 2)
                    .path(fileName)
                    .toUriString();

        } catch (IOException ex) {
            throw new FileUploadException("Impossible de stocker le fichier " + fileName, ex);
        }
    }

}