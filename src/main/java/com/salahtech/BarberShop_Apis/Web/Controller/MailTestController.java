package com.salahtech.BarberShop_Apis.Web.Controller;

import com.salahtech.BarberShop_Apis.Services.EmailService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test-mail")
@RequiredArgsConstructor
public class MailTestController {

    private final EmailService emailService;

    // GET simple (argument "to" en query param)
    // Exemple: http://localhost:8080/api/test-mail?to=destinataire@example.com
    @GetMapping
    public ResponseEntity<?> sendTestMail(@RequestParam String to) {
        try {
            String html = """
                      <div style="font-family:Segoe UI,Arial,sans-serif">
                        <h2>Test Barber Shop API</h2>
                        <p>Ça marche ! Email HTML via SMTP Gmail.</p>
                      </div>
                    """;
            emailService.sendSimpleEmail(to, "Test Barber Shop API (HTML)", html);
            return ResponseEntity.ok("OK - email envoyé à " + to);
        } catch (Exception e) {
            // très important: voir l’erreur réelle côté client
            return ResponseEntity.status(500)
                    .body("ECHEC ENVOI: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
