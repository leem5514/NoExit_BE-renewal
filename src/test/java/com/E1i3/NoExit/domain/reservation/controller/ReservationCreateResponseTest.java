package com.E1i3.NoExit.domain.reservation.controller;

import com.E1i3.NoExit.domain.chat.domain.ChatRoom;
import com.E1i3.NoExit.domain.game.domain.AgeLimit;
import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.member.domain.Member;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.reservation.domain.Reservation;
import com.E1i3.NoExit.domain.reservation.domain.ReservationStatus;
import com.E1i3.NoExit.domain.reservation.dto.ReservationCreateResDto;
import com.E1i3.NoExit.domain.reservation.dto.ReservationSaveDto;
import com.E1i3.NoExit.domain.reservation.service.ReservationService;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/* POST /reservation/create 응답에 사장님/회원 비밀번호(해시)가 직렬화되지 않음을 고정한다. */
@ExtendWith(MockitoExtension.class)
class ReservationCreateResponseTest {

    private static final String OWNER_HASH = "$2a$10$ownerPasswordHashShouldNeverLeak";
    private static final String MEMBER_HASH = "$2a$10$memberPasswordHashShouldNeverLeak";

    @Mock private ReservationService reservationService;

    private ReservationController controller;
    // Spring MVC 와 같은 방식으로 구성한 ObjectMapper (JavaTimeModule 등 자동 등록)
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @BeforeEach
    void setUp() {
        controller = new ReservationController(reservationService);
    }

    private Reservation savedReservation() {
        Owner owner = Owner.builder().id(1L).username("owner").email("owner@test.com")
                .password(OWNER_HASH).storeName("Admin Store").phoneNumber("010-1234-5678").build();
        Store store = Store.builder().id(10L).storeName("My Store").openingHours("10:00 - 22:00")
                .owner(owner).build();
        Game game = Game.builder().id(100L).gameName("Escape").runningTime("60분").difficult(Difficult.ONE)
                .price(20000).maximunPerson(4).ageLimit(AgeLimit.ADULT).store(store).build();
        Member member = Member.builder().email("user@test.com").nickname("user").password(MEMBER_HASH).build();

        Reservation reservation = Reservation.builder()
                .owner(owner).member(member).game(game)
                .resName("홍길동").phoneNumber("010-0000-0000")
                .resDate(LocalDate.of(2026, 12, 1)).resDateTime("14:00")
                .numberOfPlayers(3).reservationStatus(ReservationStatus.WAITING)
                .build();
        ReflectionTestUtils.setField(reservation, "id", 7L);
        return reservation;
    }

    @Test
    void createResponse_isDto_withoutPasswordOrOwner() throws Exception {
        when(reservationService.save(any(ReservationSaveDto.class))).thenReturn(savedReservation());

        ResponseEntity<?> response = controller.reservationCreate(new ReservationSaveDto());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        String json = objectMapper.writeValueAsString(response.getBody());
        JsonNode result = objectMapper.readTree(json).get("result");

        // 비밀번호(필드명·해시값) 및 사장님/회원 엔티티가 응답에 없어야 한다
        assertFalse(json.toLowerCase().contains("password"), json);
        assertFalse(json.contains(OWNER_HASH), json);
        assertFalse(json.contains(MEMBER_HASH), json);
        assertFalse(result.has("owner"), json);
        assertFalse(result.has("member"), json);
        assertFalse(json.contains("owner@test.com"), json);

        // 예약 정보 자체는 응답에 남아 있어야 한다
        assertEquals(7L, result.get("id").asLong());
        assertEquals(100L, result.get("gameId").asLong());
        assertEquals("Escape", result.get("gameName").asText());
        assertEquals("My Store", result.get("storeName").asText());
        assertEquals("14:00", result.get("resDateTime").asText());
        assertEquals("WAITING", result.get("reservationStatus").asText());
    }

    @Test
    void createResponse_resultType_isCreateResDto() {
        when(reservationService.save(any(ReservationSaveDto.class))).thenReturn(savedReservation());

        Object body = controller.reservationCreate(new ReservationSaveDto()).getBody();
        Object result = ((com.E1i3.NoExit.domain.common.dto.CommonResDto) body).getResult();
        assertInstanceOf(ReservationCreateResDto.class, result);
    }

    @Test
    void chatRoomSerialization_excludesPassword() throws Exception {
        ChatRoom room = new ChatRoom("방", "room-secret-1234");

        String json = objectMapper.writeValueAsString(room);

        assertFalse(json.toLowerCase().contains("password"), json);
        assertFalse(json.contains("room-secret-1234"), json);
        assertTrue(json.contains("\"name\""), json);
    }
}
