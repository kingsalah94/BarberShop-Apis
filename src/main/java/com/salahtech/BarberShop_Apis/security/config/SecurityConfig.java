package com.salahtech.BarberShop_Apis.security.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.salahtech.BarberShop_Apis.Services.CustomUserDetailsService;
import com.salahtech.BarberShop_Apis.Services.Interfaces.ApplicationUserService;
import com.salahtech.BarberShop_Apis.Services.RefreshTokenService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.security.Filter.JwtAuthenticationFilter;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            CustomUserDetailsService userDetailsService,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ⚠️ On ne garde PAS de champ @Autowired du handler pour éviter le cycle
    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(
            JwtUtil jwtUtil,
            @Lazy ApplicationUserService userService, // <— casse la boucle
            RefreshTokenService refreshTokenService) {
        return new OAuth2AuthenticationSuccessHandler(jwtUtil, userService, refreshTokenService);
    }

    private static final String[] PUBLIC_URLS = {
            // Auth
            "/v1/auth/login",
            "/v1/users",
            "/v1/permissions",
            "/v1/roles",
            "/v1/auth/register",
            "/v1/auth/refresh-token",
            "/v1/auth/forgot-password",
            "/v1/auth/reset-password",
            "/v1/auth/verify-email",
            "/v1/oauth2/**",

            // springdoc (toujours sans le context-path dans les matchers)
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**",

            // actuator de base
            "/actuator/health/**",
            "/actuator/info"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            OAuth2AuthenticationSuccessHandler successHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS preflight
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .requestMatchers("/client/**").hasAuthority("CLIENT_ACCESS")
                        .requestMatchers("/barbers/**").hasAuthority("BARBER_ACCESS")
                        .requestMatchers("/salon/**").hasAuthority("SALON_OWNER_ACCESS")
                        .requestMatchers("/admin/**").hasAuthority("ADMIN_ACCESS")
                        // Exemples par rôle
                        .requestMatchers("/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/v1/dashboard/**")
                        .hasAnyRole("ADMIN", "SALON_OWNER", "BARBER")
                        // fichiers publics
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/files/**").permitAll()
                        // upload réservé (au moins authentifié)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/v1/files").authenticated()
                        // Toutes les autres requêtes doivent être authentifiées
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(successHandler)
                        .failureUrl("/auth/login?error=oauth2_error"))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        var h = new DefaultMethodSecurityExpressionHandler();
        h.setRoleHierarchy(roleHierarchy);
        return h;
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
