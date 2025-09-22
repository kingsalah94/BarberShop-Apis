package com.salahtech.BarberShop_Apis.Services.Implementations;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salahtech.BarberShop_Apis.Services.Interfaces.SecuritySettingsService;
import com.salahtech.BarberShop_Apis.security.Dtos.SecuritySettingsDto;
import com.salahtech.BarberShop_Apis.security.models.SecuritySettings;
import com.salahtech.BarberShop_Apis.security.repositories.SecuritySettingsRepository;
import com.salahtech.BarberShop_Apis.security.utils.Csvs;

@Service
@RequiredArgsConstructor
public class SecuritySettingsServiceImpl implements SecuritySettingsService {

    private final SecuritySettingsRepository repo;

    private SecuritySettings ensure() {
        return repo.findById(1L).orElseGet(() -> {
            SecuritySettings s = SecuritySettings.builder()
                    .id(1L)
                    // defaults raisonnables
                    .passwordMinLength(8)
                    .passwordRequireUpper(true)
                    .passwordRequireLower(true)
                    .passwordRequireDigit(true)
                    .passwordRequireSymbol(false)
                    .passwordExpiryDays(0)
                    .mfaEnabled(false)
                    .mfaRequiredRolesCsv(null)
                    .mfaAllowedFactorsCsv("TOTP,EMAIL_OTP")
                    .jwtAlg("RS256")
                    .jwtAccessTtlSeconds(1800)
                    .jwtRefreshTtlSeconds(1209600)
                    .jwtKid("key-1")
                    .corsAllowedOriginsCsv("*")
                    .corsAllowCredentials(false)
                    .corsAllowedMethodsCsv("GET,POST,PUT,PATCH,DELETE,OPTIONS")
                    .corsAllowedHeadersCsv("Authorization,Content-Type")
                    .csrfEnabled(false)
                    .rateLoginPerMinute(10)
                    .rateApiPerMinute(300)
                    .captchaEnabled(false)
                    .captchaProvider("RECAPTCHA")
                    .captchaThreshold(0.5)
                    .build();
            return repo.save(s);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public SecuritySettingsDto get() {
        var s = ensure();
        return SecuritySettingsDto.builder()
                .passwordMinLength(s.getPasswordMinLength())
                .passwordRequireUpper(s.getPasswordRequireUpper())
                .passwordRequireLower(s.getPasswordRequireLower())
                .passwordRequireDigit(s.getPasswordRequireDigit())
                .passwordRequireSymbol(s.getPasswordRequireSymbol())
                .passwordExpiryDays(s.getPasswordExpiryDays())
                .mfaEnabled(s.getMfaEnabled())
                .mfaRequiredRoles(Csvs.toList(s.getMfaRequiredRolesCsv()))
                .mfaAllowedFactors(Csvs.toList(s.getMfaAllowedFactorsCsv()))
                .jwtAlg(s.getJwtAlg())
                .jwtAccessTtlSeconds(s.getJwtAccessTtlSeconds())
                .jwtRefreshTtlSeconds(s.getJwtRefreshTtlSeconds())
                .jwtKid(s.getJwtKid())
                .corsAllowedOrigins(Csvs.toList(s.getCorsAllowedOriginsCsv()))
                .corsAllowCredentials(s.getCorsAllowCredentials())
                .corsAllowedMethods(Csvs.toList(s.getCorsAllowedMethodsCsv()))
                .corsAllowedHeaders(Csvs.toList(s.getCorsAllowedHeadersCsv()))
                .csrfEnabled(s.getCsrfEnabled())
                .rateLoginPerMinute(s.getRateLoginPerMinute())
                .rateApiPerMinute(s.getRateApiPerMinute())
                .captchaEnabled(s.getCaptchaEnabled())
                .captchaProvider(s.getCaptchaProvider())
                .captchaThreshold(s.getCaptchaThreshold())
                .build();
    }

    @Override
    @Transactional
    public SecuritySettingsDto update(SecuritySettingsDto dto) {
        var s = ensure();
        s.setPasswordMinLength(dto.getPasswordMinLength());
        s.setPasswordRequireUpper(dto.getPasswordRequireUpper());
        s.setPasswordRequireLower(dto.getPasswordRequireLower());
        s.setPasswordRequireDigit(dto.getPasswordRequireDigit());
        s.setPasswordRequireSymbol(dto.getPasswordRequireSymbol());
        s.setPasswordExpiryDays(dto.getPasswordExpiryDays());

        s.setMfaEnabled(dto.getMfaEnabled());
        s.setMfaRequiredRolesCsv(Csvs.toCsv(dto.getMfaRequiredRoles()));
        s.setMfaAllowedFactorsCsv(Csvs.toCsv(dto.getMfaAllowedFactors()));

        s.setJwtAlg(dto.getJwtAlg());
        s.setJwtAccessTtlSeconds(dto.getJwtAccessTtlSeconds());
        s.setJwtRefreshTtlSeconds(dto.getJwtRefreshTtlSeconds());
        s.setJwtKid(dto.getJwtKid());

        s.setCorsAllowedOriginsCsv(Csvs.toCsv(dto.getCorsAllowedOrigins()));
        s.setCorsAllowCredentials(dto.getCorsAllowCredentials());
        s.setCorsAllowedMethodsCsv(Csvs.toCsv(dto.getCorsAllowedMethods()));
        s.setCorsAllowedHeadersCsv(Csvs.toCsv(dto.getCorsAllowedHeaders()));

        s.setCsrfEnabled(dto.getCsrfEnabled());
        s.setRateLoginPerMinute(dto.getRateLoginPerMinute());
        s.setRateApiPerMinute(dto.getRateApiPerMinute());

        s.setCaptchaEnabled(dto.getCaptchaEnabled());
        s.setCaptchaProvider(dto.getCaptchaProvider());
        s.setCaptchaThreshold(dto.getCaptchaThreshold());

        repo.save(s);
        return get();
    }

    @Override
    @Transactional
    public SecuritySettingsDto rotateKid() {
        var s = ensure();
        // génère un KID simple et unique; adapte si tu gères un keystore
        String newKid = "key-" + System.currentTimeMillis();
        s.setJwtKid(newKid);
        repo.save(s);
        return get();
    }
}
