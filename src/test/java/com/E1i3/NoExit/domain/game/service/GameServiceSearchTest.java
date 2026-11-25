package com.E1i3.NoExit.domain.game.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.dto.GameSearchDto;
import com.E1i3.NoExit.domain.game.dto.GameSearchResDto;
import com.E1i3.NoExit.domain.game.dto.GameSortType;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.game.repository.GameTimeSlotRepository;
import com.E1i3.NoExit.domain.store.domain.Store;

@ExtendWith(MockitoExtension.class)
class GameServiceSearchTest {

	@Mock private GameRepository gameRepository;
	@Mock private GameTimeSlotRepository gameTimeSlotRepository;
	private GameService gameService;

	// 중심: 강남역
	private static final double LAT = 37.4979, LNG = 127.0276;

	private final Store gangnam = store(1L, "강남점", "서울 강남구 테헤란로 1", 37.4980, 127.0280);   // ~0.04km
	private final Store seolleung = store(2L, "선릉점", "서울 강남구 선릉로 1", 37.5045, 127.0490);  // ~2.0km
	private final Store jamsil = store(3L, "잠실점", "서울 송파구 올림픽로 1", 37.5133, 127.1001);   // ~6.6km
	private final Store noCoord = store(4L, "좌표없음", "서울 강남구", null, null);

	@BeforeEach
	void setUp() {
		gameService = new GameService(gameRepository, gameTimeSlotRepository);
	}

	private static Store store(Long id, String name, String address, Double lat, Double lng) {
		return Store.builder().id(id).storeName(name).address(address).latitude(lat).longitude(lng).build();
	}

	private static Game game(Long id, Store store, int price, int minutesAgo) {
		Game g = Game.builder().id(id).gameName("게임" + id).runningTime("60분").difficult(Difficult.THREE)
			.price(price).maximunPerson(4).store(store).build();
		g.setCreatedTime(LocalDateTime.of(2026, 1, 1, 12, 0).minusMinutes(minutesAgo));
		return g;
	}

	private GameSearchDto near(double radiusKm, GameSortType sort) {
		return GameSearchDto.builder().lat(LAT).lng(LNG).radiusKm(radiusKm).sort(sort).build();
	}

	private static List<Long> ids(Page<GameSearchResDto> page) {
		return page.getContent().stream().map(GameSearchResDto::getId).collect(Collectors.toList());
	}

	@Test
	@SuppressWarnings("unchecked")
	void withoutNear_delegatesPagingAndSortToDb() {
		when(gameRepository.findAll(any(Specification.class), any(Pageable.class)))
			.thenReturn(new PageImpl<>(Collections.singletonList(game(10L, gangnam, 20000, 0))));

		GameSearchDto dto = GameSearchDto.builder().region("강남").minPrice(10000).sort(GameSortType.PRICE_DESC).build();
		Page<GameSearchResDto> result = gameService.search(dto, 2, 5);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(gameRepository).findAll(any(Specification.class), captor.capture());
		verify(gameRepository, never()).findAll(any(Specification.class));
		Pageable p = captor.getValue();
		assertEquals(2, p.getPageNumber());
		assertEquals(5, p.getPageSize());
		assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("price").getDirection());
		assertNull(result.getContent().get(0).getDistanceKm());
		assertEquals("강남점", result.getContent().get(0).getStoreName());
	}

	@Test
	@SuppressWarnings("unchecked")
	void withoutNear_defaultSortIsLatest() {
		when(gameRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

		gameService.search(new GameSearchDto(), 0, 16);

		ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
		verify(gameRepository).findAll(any(Specification.class), captor.capture());
		assertEquals(Sort.Direction.DESC, captor.getValue().getSort().getOrderFor("createdTime").getDirection());
	}

	@Test
	@SuppressWarnings("unchecked")
	void near_excludesOutsideRadiusAndMissingCoordinates() {
		// 바운딩 박스 모서리에 걸린 후보(잠실, 반경 밖)와 좌표 없는 매장도 DB가 돌려줬다고 가정
		when(gameRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(
			game(1L, gangnam, 20000, 0), game(2L, seolleung, 25000, 0),
			game(3L, jamsil, 15000, 0), game(4L, noCoord, 10000, 0)));

		Page<GameSearchResDto> result = gameService.search(near(5.0, GameSortType.DISTANCE), 0, 10);

		assertEquals(Arrays.asList(1L, 2L), ids(result));
		assertEquals(2, result.getTotalElements());
		assertTrue(result.getContent().get(0).getDistanceKm() < result.getContent().get(1).getDistanceKm());
		verify(gameRepository, never()).findAll(any(Specification.class), any(Pageable.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void near_sortsByPrice() {
		when(gameRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(
			game(1L, gangnam, 20000, 0), game(2L, seolleung, 25000, 0), game(3L, jamsil, 15000, 0)));

		assertEquals(Arrays.asList(3L, 1L, 2L), ids(gameService.search(near(10.0, GameSortType.PRICE_ASC), 0, 10)));
		assertEquals(Arrays.asList(2L, 1L, 3L), ids(gameService.search(near(10.0, GameSortType.PRICE_DESC), 0, 10)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void near_defaultSortIsLatest() {
		when(gameRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(
			game(1L, gangnam, 20000, 30), game(2L, seolleung, 25000, 0), game(3L, jamsil, 15000, 10)));

		assertEquals(Arrays.asList(2L, 3L, 1L), ids(gameService.search(near(10.0, null), 0, 10)));
	}

	@Test
	@SuppressWarnings("unchecked")
	void near_paginatesInMemory() {
		when(gameRepository.findAll(any(Specification.class))).thenReturn(Arrays.asList(
			game(1L, gangnam, 20000, 0), game(2L, seolleung, 25000, 0), game(3L, jamsil, 15000, 0)));

		Page<GameSearchResDto> second = gameService.search(near(10.0, GameSortType.DISTANCE), 1, 2);
		assertEquals(Collections.singletonList(3L), ids(second));
		assertEquals(3, second.getTotalElements());
		assertEquals(2, second.getTotalPages());

		Page<GameSearchResDto> beyond = gameService.search(near(10.0, GameSortType.DISTANCE), 5, 2);
		assertTrue(beyond.getContent().isEmpty());
		assertEquals(3, beyond.getTotalElements());
	}

	@Test
	void invalidInput_rejectedBeforeQuery() {
		assertThrows(IllegalArgumentException.class,
			() -> gameService.search(GameSearchDto.builder().sort(GameSortType.DISTANCE).build(), 0, 10));
		assertThrows(IllegalArgumentException.class, () -> gameService.search(new GameSearchDto(), -1, 10));
		assertThrows(IllegalArgumentException.class, () -> gameService.search(new GameSearchDto(), 0, 0));
		assertThrows(IllegalArgumentException.class, () -> gameService.search(new GameSearchDto(), 0, 101));
		verifyNoInteractions(gameRepository);
	}
}
