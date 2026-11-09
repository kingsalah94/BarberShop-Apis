package com.salahtech.BarberShop_Apis.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import com.salahtech.BarberShop_Apis.Config.StompAuthChannelInterceptor;
import com.salahtech.BarberShop_Apis.Config.TenantGuard;
import com.salahtech.BarberShop_Apis.Services.CustomUserDetailsService;
import com.salahtech.BarberShop_Apis.Utils.JwtUtil;
import com.salahtech.BarberShop_Apis.models.ApplicationUser;

/** Règles d'accès WebSocket : JWT au CONNECT, abonnements limités à soi / à son salon. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StompAuthChannelInterceptorTest {

    @Mock JwtUtil jwtUtil;
    @Mock CustomUserDetailsService userDetailsService;
    @Mock TenantGuard tenantGuard;
    @InjectMocks StompAuthChannelInterceptor interceptor;

    private final MessageChannel channel = mock(MessageChannel.class);
    private ApplicationUser user;

    @BeforeEach
    void setUp() {
        user = mock(ApplicationUser.class);
        when(user.getId()).thenReturn(7L);
        when(user.getUsername()).thenReturn("client@test.fr");
        when(user.getName()).thenReturn("client@test.fr");
        when(user.isEnabled()).thenReturn(true);
        when(user.getAuthorities()).thenReturn(List.of());
    }

    private Message<byte[]> frame(StompCommand cmd, String destination, String authHeader, boolean authenticated) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(cmd);
        if (destination != null) acc.setDestination(destination);
        if (authHeader != null) acc.addNativeHeader("Authorization", authHeader);
        if (authenticated) acc.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        acc.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
    }

    // ---------- CONNECT ----------

    @Test
    void connect_sansJeton_refuse() {
        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, false), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connect_jetonInvalide_refuse() {
        when(jwtUtil.extractUsername("bad")).thenThrow(new RuntimeException("signature"));

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer bad", false), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connect_jetonDeRafraichissement_refuse() {
        when(jwtUtil.extractUsername("refresh")).thenReturn("client@test.fr");
        when(userDetailsService.loadUserByUsername("client@test.fr")).thenReturn(user);
        when(jwtUtil.validateToken("refresh", user)).thenReturn(true);
        when(jwtUtil.validateAccessToken("refresh")).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer refresh", false), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void connect_jetonValide_attacheLUtilisateur() {
        when(jwtUtil.extractUsername("ok")).thenReturn("client@test.fr");
        when(userDetailsService.loadUserByUsername("client@test.fr")).thenReturn(user);
        when(jwtUtil.validateToken("ok", user)).thenReturn(true);
        when(jwtUtil.validateAccessToken("ok")).thenReturn(true);

        Message<?> out = interceptor.preSend(frame(StompCommand.CONNECT, null, "Bearer ok", false), channel);

        StompHeaderAccessor acc = StompHeaderAccessor.wrap(out);
        assertThat(acc.getUser()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
    }

    // ---------- SUBSCRIBE ----------

    @Test
    void subscribe_anonyme_refuse() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/7/inbox", null, false), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_saPropreBoite_ok() {
        assertThatCode(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/7/inbox", null, true), channel))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_boiteDUnAutre_refuse() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/8/inbox", null, true), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_boiteDUnAutre_okPourAdmin() {
        when(tenantGuard.isAdmin(user)).thenReturn(true);

        assertThatCode(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/8/inbox", null, true), channel))
                .doesNotThrowAnyException();
    }

    @Test
    void subscribe_salonHorsTenant_refuse() {
        doThrow(new AccessDeniedException("hors salon")).when(tenantGuard).assertSalonStaff(any(), anyLong());

        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/salon/3/queue", null, true), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_destinationInconnue_refuse() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/admin/all", null, true), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void subscribe_tentativeDeContournementDuMotif_refuse() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SUBSCRIBE, "/topic/user/7x/inbox", null, true), channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- SEND ----------

    @Test
    void send_toujoursRefuse() {
        assertThatThrownBy(() -> interceptor.preSend(
                frame(StompCommand.SEND, "/app/anything", null, true), channel))
                .isInstanceOf(AccessDeniedException.class);
    }
}
