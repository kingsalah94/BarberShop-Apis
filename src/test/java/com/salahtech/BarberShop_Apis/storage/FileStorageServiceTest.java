package com.salahtech.BarberShop_Apis.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.salahtech.BarberShop_Apis.Config.FileStorageProperties;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;
import com.salahtech.BarberShop_Apis.Services.Implementations.FileStorageService;

/** Upload : seules de vraies images passent, quel que soit le nom ou le Content-Type annoncé. */
class FileStorageServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    @TempDir Path uploadDir;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(uploadDir.toString());
        service = new FileStorageService();
        service.FileStorageServiceImpl(props);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @Test
    void png_accepte_extensionImposeeParLeServeur() throws Exception {
        String url = service.storeFile(new MockMultipartFile("f", "photo.html", "text/html", PNG));

        assertThat(url).endsWith(".png");
        try (var files = Files.list(uploadDir)) {
            assertThat(files.map(p -> p.getFileName().toString())).singleElement().satisfies(n -> {
                assertThat(n).endsWith(".png");
                assertThat(n).doesNotContain("photo");
            });
        }
    }

    @Test
    void jpg_et_webp_acceptes() {
        assertThat(service.storeFile(new MockMultipartFile("f", "a", null, JPG))).endsWith(".jpg");
        assertThat(service.storeFile(new MockMultipartFile("f", "b", null, WEBP))).endsWith(".webp");
    }

    @Test
    void html_deguiseEnImage_refuse() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.storeFile(new MockMultipartFile("f", "x.png", "image/png", html)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void svg_refuse() {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.storeFile(new MockMultipartFile("f", "x.svg", "image/svg+xml", svg)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void fichierVide_refuse() {
        assertThatThrownBy(() -> service.storeFile(new MockMultipartFile("f", "x.png", "image/png", new byte[0])))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void fichierTropGros_refuse() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);

        assertThatThrownBy(() -> service.storeFile(new MockMultipartFile("f", "x.png", "image/png", big)))
                .isInstanceOf(BadRequestException.class);
    }
}
