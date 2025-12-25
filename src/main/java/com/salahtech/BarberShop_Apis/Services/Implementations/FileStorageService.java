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

    // private final Path fileStorageLocation;

    // // @Autowired
    // // public FileStorageService(FileStorageProperties fileStorageProperties) {
    // // this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
    // // .toAbsolutePath().normalize();

    // // try {
    // // Files.createDirectories(this.fileStorageLocation);
    // // log.info("📁 Répertoire de stockage créé : {}", this.fileStorageLocation);
    // // } catch (Exception ex) {
    // // throw new FileUploadException("Impossible de créer le répertoire de
    // stockage", ex);
    // // }
    // // }

    // /**
    // * Stocke un fichier dans le répertoire spécifié
    // *
    // * @param file Fichier à uploader
    // * @param subDirectory Sous-répertoire (ex: "services", "barbers")
    // * @return Nom du fichier généré (UNIQUEMENT le nom, pas le chemin complet)
    // */
    // public String storeFile(MultipartFile file, String subDirectory) {
    // // Validation du fichier
    // if (file.isEmpty()) {
    // throw new FileUploadException("Le fichier est vide");
    // }

    // // Nettoyage du nom de fichier
    // String originalFileName =
    // StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

    // // Validation de l'extension
    // String fileExtension = getFileExtension(originalFileName);
    // if (!isValidImageExtension(fileExtension)) {
    // throw new FileUploadException(
    // "Type de fichier non autorisé. Extensions autorisées : jpg, jpeg, png, gif,
    // webp");
    // }

    // // Génération d'un nom unique
    // String newFileName = generateUniqueFileName(originalFileName);

    // try {
    // // Vérification de caractères invalides
    // if (originalFileName.contains("..")) {
    // throw new FileUploadException("Le nom de fichier contient une séquence de
    // chemin invalide");
    // }

    // // Création du sous-répertoire si nécessaire
    // Path targetLocation = this.fileStorageLocation.resolve(subDirectory);
    // Files.createDirectories(targetLocation);

    // // Copie du fichier vers la destination
    // Path destinationFile = targetLocation.resolve(newFileName);
    // Files.copy(file.getInputStream(), destinationFile,
    // StandardCopyOption.REPLACE_EXISTING);

    // log.info("✅ Fichier uploadé avec succès : {} dans {}", newFileName,
    // subDirectory);

    // // ✅ CORRECTION: Retourner UNIQUEMENT le nom du fichier
    // // Pas "subDirectory/file.jpg" mais juste "file.jpg"
    // return newFileName;

    // } catch (IOException ex) {
    // throw new FileUploadException("Erreur lors du stockage du fichier " +
    // newFileName, ex);
    // }
    // }

    // /**
    // * Charge un fichier comme ressource
    // *
    // * @param subDirectory Sous-répertoire (ex: "services")
    // * @param fileName Nom du fichier
    // * @return Ressource du fichier
    // */
    // public Resource loadFileAsResource(String subDirectory, String fileName) {
    // try {
    // // Construire le chemin complet : uploads/services/file.jpg
    // Path filePath =
    // this.fileStorageLocation.resolve(subDirectory).resolve(fileName).normalize();
    // Resource resource = new UrlResource(filePath.toUri());

    // if (resource.exists() && resource.isReadable()) {
    // return resource;
    // } else {
    // throw new FileUploadException("Fichier introuvable : " + subDirectory + "/" +
    // fileName);
    // }
    // } catch (MalformedURLException ex) {
    // throw new FileUploadException("Fichier introuvable : " + subDirectory + "/" +
    // fileName, ex);
    // }
    // }

    // /**
    // * Supprime un fichier
    // *
    // * @param subDirectory Sous-répertoire
    // * @param fileName Nom du fichier à supprimer
    // * @return true si supprimé, false sinon
    // */
    // public boolean deleteFile(String subDirectory, String fileName) {
    // try {
    // Path filePath =
    // this.fileStorageLocation.resolve(subDirectory).resolve(fileName).normalize();
    // boolean deleted = Files.deleteIfExists(filePath);

    // if (deleted) {
    // log.info("🗑️ Fichier supprimé : {}/{}", subDirectory, fileName);
    // } else {
    // log.warn("⚠️ Fichier non trouvé pour suppression : {}/{}", subDirectory,
    // fileName);
    // }

    // return deleted;
    // } catch (IOException ex) {
    // log.error("❌ Erreur lors de la suppression du fichier : {}/{}", subDirectory,
    // fileName, ex);
    // return false;
    // }
    // }

    // /**
    // * ✅ NOUVELLE MÉTHODE: Extrait uniquement le nom du fichier depuis un chemin
    // * Ex: "BarberShop-Apis/uploads/services/file.jpg" → "file.jpg"
    // */
    // public String extractFileName(String path) {
    // if (path == null || path.isEmpty()) {
    // return path;
    // }

    // // Remplacer les backslashes par des slashes
    // path = path.replace("\\", "/");

    // // Extraire le dernier segment après le dernier /
    // int lastSlash = path.lastIndexOf("/");
    // if (lastSlash >= 0) {
    // return path.substring(lastSlash + 1);
    // }

    // return path;
    // }

    // /**
    // * Génère un nom de fichier unique
    // */
    // private String generateUniqueFileName(String originalFileName) {
    // String fileExtension = getFileExtension(originalFileName);
    // String nameWithoutExtension = originalFileName.substring(0,
    // originalFileName.lastIndexOf('.'));

    // // Format: original-name-uuid.ext
    // String uuid = UUID.randomUUID().toString().substring(0, 8);
    // return sanitizeFileName(nameWithoutExtension) + "-" + uuid + "." +
    // fileExtension;
    // }

    // /**
    // * Nettoie le nom de fichier (supprime les caractères spéciaux)
    // */
    // private String sanitizeFileName(String fileName) {
    // return fileName.replaceAll("[^a-zA-Z0-9.-]", "-").toLowerCase();
    // }

    // /**
    // * Récupère l'extension d'un fichier
    // */
    // private String getFileExtension(String fileName) {
    // if (fileName.lastIndexOf(".") > 0) {
    // return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    // }
    // return "";
    // }

    // /**
    // * Vérifie si l'extension est une image valide
    // */
    // private boolean isValidImageExtension(String extension) {
    // return extension.matches("jpg|jpeg|png|gif|webp");
    // }

    // @Autowired
    // public FileStorageServiceImpl(FileStorageProperties fileStorageProperties) {
    // this.fileStorageLocation = Paths.get(fileStorageProperties.getUploadDir())
    // .toAbsolutePath().normalize();

    // try {
    // Files.createDirectories(this.fileStorageLocation);
    // } catch (Exception ex) {
    // throw new FileUploadException("Impossible de créer le dossier d'upload.",
    // ex);
    // }
    // }

    // public String storeFile(MultipartFile file) {
    // // 1. Nettoyer le nom du fichier
    // String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());

    // // 2. Générer un nom unique (UUID) pour éviter les conflits
    // // Ex: "avatar.jpg" devient "a1b2c3d4-avatar.jpg"
    // String fileExtension = "";
    // try {
    // fileExtension =
    // originalFileName.substring(originalFileName.lastIndexOf("."));
    // } catch(Exception e) {
    // fileExtension = "";
    // }
    // String fileName = UUID.randomUUID().toString() + fileExtension;

    // try {
    // // Vérification de sécurité (pas de ".." dans le chemin)
    // if(fileName.contains("..")) {
    // throw new FileUploadException("Nom de fichier invalide " + fileName);
    // }

    // // 3. Copier le fichier
    // Path targetLocation = this.fileStorageLocation.resolve(fileName);
    // Files.copy(file.getInputStream(), targetLocation,
    // StandardCopyOption.REPLACE_EXISTING);

    // // 4. Retourner l'URL complète accessible
    // return ServletUriComponentsBuilder.fromCurrentContextPath()
    // .path("/uploads/") // Doit correspondre à la config WebMvc (voir étape 2)
    // .path(fileName)
    // .toUriString();

    // } catch (IOException ex) {
    // throw new FileUploadException("Impossible de stocker le fichier " + fileName,
    // ex);
    // }
    // }
}