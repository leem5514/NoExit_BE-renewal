package com.E1i3.NoExit.domain.reservation.service;

import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.game.service.GameService;
import com.E1i3.NoExit.domain.member.domain.Member;
import com.E1i3.NoExit.domain.member.repository.MemberRepository;
import com.E1i3.NoExit.domain.notification.controller.SseController;
import com.E1i3.NoExit.domain.notification.repository.NotificationRepository;
import com.E1i3.NoExit.domain.notification.service.NotificationService;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.reservation.domain.Reservation;
import com.E1i3.NoExit.domain.reservation.dto.ReservationSaveDto;
import com.E1i3.NoExit.domain.reservation.repository.ReservationRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/* 형식만 다른 예약 시각("10:00" / "10:00:00")이 같은 시간대로 취급되어 중복 예약이 거부됨을 고정한다. */
@ExtendWith(MockitoExtension.class)
class ReservationTimeNormalizeTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private GameRepository gameRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SseController sseController;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private GameService gameService;
    @Mock private RedisTemplate<String, Object> reservationRedisTemplate;
    @Mock private ValueOperations<String, Object> valueOps;

    private ReservationService reservationService;
    // setIfAbsent 동작을 흉내내는 인메모리 Redis
    private final Map<String, Object> redis = new HashMap<>();

    private final Owner owner = Owner.builder().id(1L).email("owner@test.com").storeName("My Store").build();
    private final Store store = Store.builder().id(10L).storeName("My Store").owner(owner).build();
    private final Game game = Game.builder().id(100L).gameName("Escape").store(store).build();
    private final Member member = Member.builder().email("user@test.com").nickname("user").build();
    private final LocalDate date = LocalDate.of(2026, 12, 1);

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, gameRepository, memberRepository,
                ownerRepository, storeRepository, sseController, notificationRepository, notificationService, gameService);
        ReflectionTestUtils.setField(reservationService, "reservationRedisTemplate", reservationRedisTemplate);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@test.com", null));

        when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
        when(gameRepository.findById(100L)).thenReturn(Optional.of(game));
        when(gameService.getAvailableHours(game)).thenReturn(List.of(LocalTime.of(10, 0), LocalTime.of(11, 0)));
        when(reservationRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), any(), anyLong(), any()))
                .thenAnswer(inv -> redis.putIfAbsent(inv.getArgument(0), inv.getArgument(1)) == null);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ReservationSaveDto request(String time) {
        ReservationSaveDto dto = new ReservationSaveDto();
        dto.setGameId(100L);
        dto.setResDate(date);
        dto.setResDateTime(time);
        dto.setResName("홍길동");
        dto.setNumberOfPlayers(2);
        return dto;
    }

    @Test
    void sameSlot_inDifferentFormats_secondIsRejected() {
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation first = reservationService.save(request("10:00:00"));
        assertEquals("10:00", first.getResDateTime());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> reservationService.save(request("10:00")));
        assertTrue(ex.getMessage().contains("이미 예약 중인 시간대"));

        // 잠금 키는 정규화된 값 하나만 존재하고, 저장도 한 번만 일어난다
        assertEquals(1, redis.size());
        assertTrue(redis.containsKey("reservation:lock:My Store:Escape:2026-12-01:10:00"));
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void storedValue_andLockKey_areNormalizedToHHmm() {
        ArgumentCaptor<Reservation> captor = ArgumentCaptor.forClass(Reservation.class);
        when(reservationRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        reservationService.save(request(" 11:00:00 "));

        assertEquals("11:00", captor.getValue().getResDateTime());
        verify(valueOps).set(eq("reservation:lock:My Store:Escape:2026-12-01:11:00"), eq("RESERVED"), anyLong(), any());
    }
}
