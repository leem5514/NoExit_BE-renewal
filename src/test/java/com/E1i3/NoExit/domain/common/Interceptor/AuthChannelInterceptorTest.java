package com.E1i3.NoExit.domain.common.Interceptor;

import com.E1i3.NoExit.domain.common.auth.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/* STOMP CONNECT 처리 중 액세스 토큰 원문이 표준 출력(로그)에 남지 않음을 고정한다. */
@ExtendWith(MockitoExtension.class)
class AuthChannelInterceptorTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.secretPayload.secretSignature";

    @Mock private JwtTokenProvider jwtTokenProvider;

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        SecurityContextHolder.clearContext();
    }

    private Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void validToken_isNotLogged() {
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(TOKEN)).thenReturn(
                new UsernamePasswordAuthenticationToken("user@test.com", "", Collections.emptyList()));

        new AuthChannelInterceptor(jwtTokenProvider).preSend(connect("Bearer " + TOKEN), mock(MessageChannel.class));

        String out = captured.toString(StandardCharsets.UTF_8);
        assertFalse(out.contains(TOKEN), out);
        assertFalse(out.contains("secretSignature"), out);
        assertEquals("user@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void invalidToken_isNotLoggedEither() {
        when(jwtTokenProvider.validateToken(TOKEN)).thenReturn(false);

        AuthChannelInterceptor interceptor = new AuthChannelInterceptor(jwtTokenProvider);
        assertThrows(Exception.class,
                () -> interceptor.preSend(connect("Bearer " + TOKEN), mock(MessageChannel.class)));

        String out = captured.toString(StandardCharsets.UTF_8);
        assertFalse(out.contains(TOKEN), out);
    }
}
