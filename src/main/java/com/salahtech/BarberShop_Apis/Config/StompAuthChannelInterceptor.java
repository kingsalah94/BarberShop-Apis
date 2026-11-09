package com.salahtech.BarberShop_Apis.Config;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.salahtech.BarberShop_Apis.Services.CustomUserDetailsService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Sécurité STOMP (le handshake HTTP /ws/** est public, la sécurité est ici) :
 * <ul>
 *   <li>CONNECT : JWT d'accès obligatoire (header natif {@code Authorization: Bearer ...}).</li>
 *   <li>SUBSCRIBE : autorisation par destination
 *     <ul>
 *       <li>{@code /topic/user/{id}/**} : uniquement l'utilisateur {@code id} lui-même ;</li>
 *       <li>{@code /topic/salon/{id}/**} : personnel du salon (TenantGuard.isSalonStaff) ou ADMIN ;</li>
 *       <li>tout le reste : refusé.</li>
 *     </ul></li>
 *   <li>SEND : refusé (aucun {@code @MessageMapping} côté serveur, flux sortant uniquement).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern USER_TOPIC = Pattern.compile("^/topic/user/(\\d+)(/.*)?$");
    private static final Pattern SALON_TOPIC = Pattern.compile("^/topic/salon/(\\d+)(/.*)?$");

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TenantGuard tenantGuard;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();
        try {
            switch (command) {
                case CONNECT -> authenticate(accessor);
                case SUBSCRIBE -> authorizeSubscribe(accessor);
                case SEND -> throw new AccessDeniedException("Envoi STOMP non autorisé");
                default -> { /* DISCONNECT, UNSUBSCRIBE, ACK... : rien à contrôler */ }
            }
        } catch (RuntimeException e) {
            log.warn("STOMP {} refusé (dest={}, user={}) : {}", command, accessor.getDestination(),
                    accessor.getUser() != null ? accessor.getUser().getName() : null, e.toString());
            throw e;
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("Jeton manquant");
        }
        String token = header.substring(7);
        try {
            String username = jwtUtil.extractUsername(token);
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (!jwtUtil.validateToken(token, user) || !jwtUtil.validateAccessToken(token) || !user.isEnabled()) {
                throw new AccessDeniedException("Jeton invalide");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.debug("CONNECT STOMP refusé : {}", e.getMessage());
            throw new AccessDeniedException("Jeton invalide");
        }
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        ApplicationUser user = currentUser(accessor.getUser());
        String dest = accessor.getDestination();
        if (dest == null) throw new AccessDeniedException("Destination manquante");

        Matcher m = USER_TOPIC.matcher(dest);
        if (m.matches()) {
            Long targetId = Long.valueOf(m.group(1));
            if (!targetId.equals(user.getId()) && !tenantGuard.isAdmin(user)) {
                throw new AccessDeniedException("Abonnement refusé");
            }
            return;
        }

        m = SALON_TOPIC.matcher(dest);
        if (m.matches()) {
            // lève AccessDeniedException si l'utilisateur n'est pas personnel du salon (ADMIN : bypass)
            tenantGuard.assertSalonStaff(user, Long.valueOf(m.group(1)));
            return;
        }

        throw new AccessDeniedException("Abonnement refusé");
    }

    private ApplicationUser currentUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof ApplicationUser u) {
            return u;
        }
        throw new AccessDeniedException("Non authentifié");
    }
}
