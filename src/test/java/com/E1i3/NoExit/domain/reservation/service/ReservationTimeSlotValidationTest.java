package com.E1i3.NoExit.domain.reservation.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

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

@ExtendWith(MockitoExtension.class)
class ReservationTimeSlotValidationTest {

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
	@Mock private ValueOperations<String, Object> valueOperations;

	private ReservationService reservationService;
	private Game game;

	@BeforeEach
	void setUp() {
		reservationService = new ReservationService(reservationRepository, gameRepository, memberRepository,
			ownerRepository, storeRepository, sseController, notificationRepository, notificationService, gameService);
		ReflectionTestUtils.setField(reservationService, "reservationRedisTemplate", reservationRedisTemplate);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("user@test.com", null));

		Owner owner = Owner.builder().id(1L).email("owner@test.com").build();
		Store store = Store.builder().id(10L).storeName("S").openingHours("10:00 - 22:00").owner(owner).build();
		game = Game.builder().id(100L).gameName("G").runningTime("60분").store(store).build();
		when(memberRepository.findByEmail("user@test.com"))
			.thenReturn(Optional.of(Member.builder().email("user@test.com").nickname("u").build()));
		when(gameRepository.findById(100L)).thenReturn(Optional.of(game));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private ReservationSaveDto request(String time) {
		ReservationSaveDto dto = new ReservationSaveDto();
		dto.setGameId(100L);
		dto.setResName("홍길동");
		dto.setNumberOfPlayers(2);
		dto.setResDate(LocalDate.of(2026, 12, 1));
		dto.setResDateTime(time);
		return dto;
	}

	@Test
	void time_outside_available_slots_is_rejected_before_locking() {
		when(gameService.getAvailableHours(game)).thenReturn(List.of(LocalTime.of(10, 30), LocalTime.of(12, 0)));

		assertThrows(IllegalArgumentException.class, () -> reservationService.save(request("11:00")));
		verifyNoInteractions(reservationRedisTemplate);
		verify(reservationRepository, never()).save(any());
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(strings = {"", "1030", "오전 10시", "25:00"})
	void unparseable_time_is_rejected_before_locking(String time) {
		assertThrows(IllegalArgumentException.class, () -> reservationService.save(request(time)));
		verifyNoInteractions(reservationRedisTemplate);
		verify(reservationRepository, never()).save(any());
	}

	@Test
	void time_in_available_slots_proceeds_to_lock_and_save() {
		when(gameService.getAvailableHours(game)).thenReturn(List.of(LocalTime.of(10, 30), LocalTime.of(12, 0)));
		when(reservationRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(anyString(), any(), eq(3L), eq(TimeUnit.HOURS))).thenReturn(true);
		when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

		Reservation saved = reservationService.save(request("10:30"));

		assertEquals("10:30", saved.getResDateTime());
		verify(reservationRepository).save(any(Reservation.class));
	}
}
