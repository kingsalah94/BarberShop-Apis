package com.salahtech.BarberShop_Apis.Utils;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // ⚠️ Utiliser un seul nom de propriété. Ici je garde le tiret: jwt.secret-base64
    @Value("${jwt.secret-base64:}")
    private String secretBase64;

    // (optionnel) algo configurable: HS256/HS384/HS512. Par défaut HS512.
    @Value("${jwt.algorithm:HS512}")
    private String alg;

    @Value("${jwt.access-token-expiration:900000}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;

    private SecretKey signingKey;
    private SignatureAlgorithm signatureAlgorithm;

    @PostConstruct
    void init() {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException("jwt.secret-base64 manquant. Fournis une clé Base64.");
        }

        signatureAlgorithm = SignatureAlgorithm.forName(alg);

        byte[] keyBytes = Decoders.BASE64.decode(secretBase64);

        int minBytes = switch (signatureAlgorithm) {
            case HS256 -> 32; // 256 bits
            case HS384 -> 48; // 384 bits
            case HS512 -> 64; // 512 bits
            default -> throw new IllegalStateException("Algorithme non supporté: " + signatureAlgorithm);
        };

        if (keyBytes.length < minBytes) {
            throw new IllegalStateException("Clé JWT trop courte pour " + signatureAlgorithm +
                    " : " + keyBytes.length + " octets, requis >= " + minBytes + ".");
        }

        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }
    public Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }

    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("tokenType", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expiré", e);
        } catch (UnsupportedJwtException e) {
            throw new RuntimeException("Token non supporté", e);
        } catch (MalformedJwtException e) {
            throw new RuntimeException("Token malformé", e);
        } catch (SecurityException e) {
            throw new RuntimeException("Signature invalide", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Token vide", e);
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "ACCESS");
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    public String generateAccessToken(com.salahtech.BarberShop_Apis.models.ApplicationUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "ACCESS");
        return createToken(claims, user.getEmail(), accessTokenExpiration);
    }

    public String generateRefreshToken(com.salahtech.BarberShop_Apis.models.ApplicationUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "REFRESH");
        return createToken(claims, user.getEmail(), refreshTokenExpiration);
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationMs) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, signatureAlgorithm)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        try {
            return "ACCESS".equals(extractTokenType(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            return "REFRESH".equals(extractTokenType(token)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
}
