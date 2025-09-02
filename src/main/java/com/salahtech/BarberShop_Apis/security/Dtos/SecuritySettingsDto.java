package com.salahtech.BarberShop_Apis.security.Dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritySettingsDto {

    // Password
    @Min(4)
    @Max(128)
    private Integer passwordMinLength;
    private Boolean passwordRequireUpper;
    private Boolean passwordRequireLower;
    private Boolean passwordRequireDigit;
    private Boolean passwordRequireSymbol;
    @Min(0)
    @Max(3650)
    private Integer passwordExpiryDays;

    // MFA
    private Boolean mfaEnabled;
    private List<String> mfaRequiredRoles; // ["ADMIN","MANAGER"]
    private List<String> mfaAllowedFactors; // ["TOTP","EMAIL_OTP"]

    // JWT
    @Pattern(regexp = "RS256|ES256|HS256")
    private String jwtAlg;
    @Min(60)
    @Max(86400)
    private Integer jwtAccessTtlSeconds;
    @Min(3600)
    @Max(31536000)
    private Integer jwtRefreshTtlSeconds;
    private String jwtKid;

    // CORS
    private List<@Pattern(regexp = "https?://.*") String> corsAllowedOrigins;
    private Boolean corsAllowCredentials;
    private List<String> corsAllowedMethods;
    private List<String> corsAllowedHeaders;

    // CSRF
    private Boolean csrfEnabled;

    // Rate limit
    @Min(1)
    @Max(10000)
    private Integer rateLoginPerMinute;
    @Min(1)
    @Max(100000)
    private Integer rateApiPerMinute;

    // Captcha
    private Boolean captchaEnabled;
    @Pattern(regexp = "RECAPTCHA|HCAPTCHA")
    private String captchaProvider;
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double captchaThreshold;
}
