package com.E1i3.NoExit.domain.member.controller;

import com.E1i3.NoExit.domain.common.auth.JwtTokenProvider;
import com.E1i3.NoExit.domain.member.domain.Member;
import com.E1i3.NoExit.domain.member.dto.MemberUpdateDto;
import com.E1i3.NoExit.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/* /member/myInfo, /member/delete 응답에 회원 비밀번호(해시)가 직렬화되지 않음을 고정한다. */
@ExtendWith(MockitoExtension.class)
class MemberResponseLeakTest {

    private static final String MEMBER_HASH = "$2a$10$memberPasswordHashShouldNeverLeak";

    @Mock private MemberService memberService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisTemplate<String, Object> redisTemplate;

    private MemberController controller;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        controller = new MemberController(memberService, jwtTokenProvider, redisTemplate);
    }

    private Member member() {
        return Member.builder().id(5L).username("user").nickname("user").email("user@test.com")
                .password(MEMBER_HASH).age(23).phone_number("010-1234-5678").build();
    }

    private void assertNoPassword(String json) {
        assertFalse(json.toLowerCase().contains("password"), json);
        assertFalse(json.contains(MEMBER_HASH), json);
    }

    @Test
    void myInfo_responseHasNoPassword() throws Exception {
        // 실제 엔티티 → DTO 매핑(detFromEntity)을 거친 값을 응답으로 사용
        when(memberService.myInfo()).thenReturn(member().detFromEntity());

        String json = objectMapper.writeValueAsString(controller.myInfo().getBody());
        JsonNode result = objectMapper.readTree(json).get("result");

        assertNoPassword(json);
        assertEquals("user@test.com", result.get("email").asText());
        assertEquals("user", result.get("nickname").asText());
    }

    @Test
    void delete_responseIsIdOnly() throws Exception {
        Member member = member();
        when(memberService.memberDelete()).thenReturn(member.updateDelYN());

        String json = objectMapper.writeValueAsString(controller.deleteMember(new MemberUpdateDto()).getBody());

        assertNoPassword(json);
        assertEquals(5L, objectMapper.readTree(json).get("result").asLong());
    }
}
