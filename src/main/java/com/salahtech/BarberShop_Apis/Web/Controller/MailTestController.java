package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/test/mail") // Changé en v1 pour la cohérence
@RequiredArgsConstructor
@Tag(name = "System Test", description = "Tests techniques")
public class MailTestController {

    private final EmailService emailService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 🔒 Sécurité ajoutée
    public ResponseEntity<?> sendTestMail(@RequestParam String to) {
        try {
            String html = "<h2>Test Barber Shop API</h2><p>Email HTML OK.</p>";
            emailService.sendSimpleEmail(to, "Test Barber Shop API (HTML)", html);
            return ResponseEntity.ok("OK - email envoyé à " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("ECHEC ENVOI: " + e.getMessage());
        }
    }
}