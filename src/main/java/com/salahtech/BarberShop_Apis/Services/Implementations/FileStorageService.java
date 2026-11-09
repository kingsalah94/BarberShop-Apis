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
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.Exceptions.FileUploadException;

import java.io.IOException;
import java.io.InputStream;
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

    /** Taille max d'une image (le multipart Spring limite déjà la requête). */
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    /**
     * Stocke une IMAGE uploadée.
     *
     * <p>Sécurité : le type est déterminé par les octets du fichier (magic bytes),
     * jamais par l'extension ou le Content-Type fournis par le client. Seuls
     * JPEG / PNG / GIF / WebP sont acceptés, et l'extension est imposée par le
     * serveur → impossible de déposer du HTML/SVG/JS servi ensuite sur l'origine
     * de l'API (XSS stocké).</p>
     */
    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Fichier vide");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image trop volumineuse (max 10 Mo)");
        }

        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException ex) {
            throw new FileUploadException("Lecture du fichier impossible", ex);
        }
        String extension = detectImageExtension(head, read);
        if (extension == null) {
            throw new BadRequestException("Format non supporté : seules les images JPEG, PNG, GIF et WebP sont acceptées");
        }

        String fileName = UUID.randomUUID() + extension;
        try {
            Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new FileUploadException("Chemin de stockage invalide");
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/") // Doit correspondre à la config WebMvc
                    .path(fileName)
                    .toUriString();
        } catch (IOException ex) {
            throw new FileUploadException("Impossible de stocker le fichier " + fileName, ex);
        }
    }

    /** Extension sûre selon la signature binaire, ou null si ce n'est pas une image autorisée. */
    static String detectImageExtension(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return ".jpg";
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) return ".png";
        if (n >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') return ".gif";
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') return ".webp";
        return null;
    }

}