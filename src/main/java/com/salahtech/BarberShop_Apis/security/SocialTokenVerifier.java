package com.salahtech.BarberShop_Apis.security;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Enums.AuthProvider;
import com.salahtech.BarberShop_Apis.Exceptions.BadRequestException;

/**
 * Vérifie un jeton d'identité (ID token OpenID Connect) émis par Google ou Apple :
 * signature (clés publiques JWKS du fournisseur), émetteur, audience (nos identifiants client) et expiration.
 * Aucune clé secrète n'est nécessaire côté serveur pour cette vérification.
 */
@Component
public class SocialTokenVerifier {

    /** Identité vérifiée extraite du jeton. */
    public record SocialIdentity(AuthProvider provider, String subject, String email, boolean emailVerified, String name) {}

    private static final String GOOGLE_JWKS = "https://www.googleapis.com/oauth2/v3/certs";
    private static final List<String> GOOGLE_ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");
    private static final String APPLE_JWKS = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private final List<String> googleAudiences;
    private final List<String> appleAudiences;
    private final Map<AuthProvider, JwtDecoder> decoders = new ConcurrentHashMap<>();

    public SocialTokenVerifier(@Value("${app.social.google.client-ids:}") String googleIds,
                               @Value("${app.social.apple.client-ids:}") String appleIds) {
        this.googleAudiences = split(googleIds);
        this.appleAudiences = split(appleIds);
    }

    public boolean googleEnabled() { return !googleAudiences.isEmpty(); }
    public boolean appleEnabled() { return !appleAudiences.isEmpty(); }

    public SocialIdentity verify(AuthProvider provider, String idToken) {
        if (idToken == null || idToken.isBlank()) throw new BadRequestException("Jeton d'identité manquant.");
        if (provider == AuthProvider.GOOGLE && !googleEnabled()) throw new BadRequestException("Connexion Google non configurée sur le serveur.");
        if (provider == AuthProvider.APPLE && !appleEnabled()) throw new BadRequestException("Connexion Apple non configurée sur le serveur.");
        if (provider != AuthProvider.GOOGLE && provider != AuthProvider.APPLE) throw new BadRequestException("Fournisseur non pris en charge.");

        Jwt jwt;
        try {
            jwt = decoders.computeIfAbsent(provider, this::buildDecoder).decode(idToken);
        } catch (JwtException e) {
            throw new BadRequestException("Jeton d'identité invalide ou expiré.");
        }
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) throw new BadRequestException("Le fournisseur n'a pas transmis d'adresse e-mail.");
        Object verified = jwt.getClaims().get("email_verified");
        boolean emailVerified = Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified));
        return new SocialIdentity(provider, jwt.getSubject(), email.trim().toLowerCase(), emailVerified, jwt.getClaimAsString("name"));
    }

    private JwtDecoder buildDecoder(AuthProvider provider) {
        boolean google = provider == AuthProvider.GOOGLE;
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(google ? GOOGLE_JWKS : APPLE_JWKS).build();
        List<String> audiences = google ? googleAudiences : appleAudiences;
        OAuth2TokenValidator<Jwt> issuer = jwt -> {
            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : jwt.getClaimAsString("iss");
            boolean ok = google ? GOOGLE_ISSUERS.contains(iss) : APPLE_ISSUER.equals(iss);
            return ok ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Émetteur inattendu", null));
        };
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().stream().anyMatch(audiences::contains)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Audience inattendue", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), issuer, audience));
        return decoder;
    }

    private static List<String> split(String raw) {
        return raw == null ? List.of() : Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
