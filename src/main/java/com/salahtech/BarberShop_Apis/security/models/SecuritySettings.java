package com.salahtech.BarberShop_Apis.security.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "security_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettings {

    @Id
    private Long id; // toujours 1

    // Password policy
    private Integer passwordMinLength;
    private Boolean passwordRequireUpper;
    private Boolean passwordRequireLower;
    private Boolean passwordRequireDigit;
    private Boolean passwordRequireSymbol;
    private Integer passwordExpiryDays;

    // MFA
    private Boolean mfaEnabled;
    @Column(length = 1000)
    private String mfaRequiredRolesCsv; // ex: "ADMIN,MANAGER"
    @Column(length = 1000)
    private String mfaAllowedFactorsCsv; // ex: "TOTP,EMAIL_OTP"

    // JWT / sessions
    private String jwtAlg; // ex: RS256
    private Integer jwtAccessTtlSeconds; // ex: 1800
    private Integer jwtRefreshTtlSeconds; // ex: 1209600
    private String jwtKid;

    // CORS
    @Column(length = 4000)
    private String corsAllowedOriginsCsv; // ex: "https://app.example.com,https://admin.example.com"
    private Boolean corsAllowCredentials;
    @Column(length = 2000)
    private String corsAllowedMethodsCsv; // "GET,POST,PUT,DELETE"
    @Column(length = 2000)
    private String corsAllowedHeadersCsv; // "Authorization,Content-Type"

    // CSRF
    private Boolean csrfEnabled; // simple ON/OFF

    // Rate limit (basique)
    private Integer rateLoginPerMinute; // ex: 10
    private Integer rateApiPerMinute; // ex: 300

    // reCAPTCHA / hCaptcha
    private Boolean captchaEnabled;
    private String captchaProvider; // "RECAPTCHA" | "HCAPTCHA"
    private Double captchaThreshold; // ex: 0.5
}
