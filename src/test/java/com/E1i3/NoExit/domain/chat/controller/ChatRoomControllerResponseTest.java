package com.E1i3.NoExit.domain.chat.controller;

import com.E1i3.NoExit.domain.chat.domain.ChatRoom;
import com.E1i3.NoExit.domain.chat.dto.CreateRoomRequest;
import com.E1i3.NoExit.domain.chat.service.ChatRoomService;
import com.E1i3.NoExit.domain.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* 채팅방 네 개 응답 경로에서 방 비밀번호가 직렬화되지 않고, 생성 요청의 비밀번호는 서비스에 전달됨을 고정한다. */
@ExtendWith(MockitoExtension.class)
class ChatRoomControllerResponseTest {

    // 가짜 채팅방 비밀번호 (응답에 이 문자열이 없어야 함)
    private static final String ROOM_PW_FIXTURE = "fixture room pw 1234";

    @Mock private ChatRoomService chatRoomService;
    @Mock private ChatService chatService;

    private ChatRoomController controller;
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        controller = new ChatRoomController(chatRoomService, chatService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ChatRoom room() {
        ChatRoom room = new ChatRoom("방", ROOM_PW_FIXTURE);
        room.setRoomId(1L);
        return room;
    }

    private void assertNoPassword(Object body) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        assertFalse(json.toLowerCase().contains("password"), json);
        assertFalse(json.contains(ROOM_PW_FIXTURE), json);
        assertTrue(json.contains("\"roomId\":1"), json);
    }

    @Test
    void createRoom_passesPasswordToService_butDoesNotReturnIt() throws Exception {
        // 요청 JSON 역직렬화 경로도 실제로 거친다 (ChatRoom @JsonIgnore 가 요청 DTO에 영향 없음)
        CreateRoomRequest request = objectMapper.readValue(
                "{\"name\":\"방\",\"password\":\"" + ROOM_PW_FIXTURE + "\"}", CreateRoomRequest.class);
        when(chatRoomService.createRoom("방", ROOM_PW_FIXTURE)).thenReturn(room());

        Object body = controller.createRoom(request).getBody();

        verify(chatRoomService).createRoom("방", ROOM_PW_FIXTURE);
        assertNoPassword(body);
    }

    @Test
    void roomList_hasNoPassword() throws Exception {
        when(chatRoomService.findAllRooms()).thenReturn(List.of(room()));
        assertNoPassword(controller.roomList().getBody());
    }

    @Test
    void getRoom_hasNoPassword() throws Exception {
        when(chatRoomService.findRoomById(1L)).thenReturn(room());
        assertNoPassword(controller.getRoom(1L).getBody());
    }

    @Test
    void myRooms_hasNoPassword() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", "", Collections.emptyList()));
        when(chatRoomService.getChatRoomsForMember("user@test.com")).thenReturn(List.of(room()));
        assertNoPassword(controller.getMyChatRooms().getBody());
    }
}
