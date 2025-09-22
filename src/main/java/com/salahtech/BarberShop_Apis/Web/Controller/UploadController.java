package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Dtos.UploadResponse;
import com.salahtech.BarberShop_Apis.Services.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UploadController {

    private final FileStorageService storage;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    /**
     * POST /v1/files?folder=services|barbers|users|clients
     * Body: multipart/form-data (file)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "misc") String folder,
            HttpServletRequest request) throws Exception {

        // Sécuriser le nom du "folder" (alpha-num + tirets/underscores)
        folder = sanitizeFolder(folder);

        String relative = storage.saveImage(file, folder); // ex: "services/uuid.png"
        String base = StringUtils.hasText(publicBaseUrl)
                ? trimSlash(publicBaseUrl)
                : trimSlash(ServletUriComponentsBuilder.fromRequestUri(request)
                        .replacePath(null).build().toUriString());

        String url = base + "/files/" + relative;
        return ResponseEntity.ok(new UploadResponse(url));
    }

    private String sanitizeFolder(String f) {
        String s = (f == null ? "" : f).toLowerCase().replaceAll("[^a-z0-9_\\-\\/]", "");
        if (s.isBlank())
            s = "misc";
        return s;
    }

    private String trimSlash(String s) {
        if (s.endsWith("/"))
            return s.substring(0, s.length() - 1);
        return s;
    }
}
