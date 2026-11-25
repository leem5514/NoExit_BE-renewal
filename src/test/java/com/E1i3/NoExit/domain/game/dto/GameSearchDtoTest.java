package com.E1i3.NoExit.domain.game.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameSearchDtoTest {

	@Test
	void empty_isValid_andDefaultsToLatest() {
		GameSearchDto dto = new GameSearchDto();
		assertDoesNotThrow(dto::validate);
		assertFalse(dto.hasNear());
		assertEquals(GameSortType.LATEST, dto.sortOrDefault());
	}

	@Test
	void priceRange_reversed_rejected() {
		GameSearchDto dto = GameSearchDto.builder().minPrice(30000).maxPrice(20000).build();
		assertThrows(IllegalArgumentException.class, dto::validate);
	}

	@Test
	void priceRange_equal_ok() {
		assertDoesNotThrow(GameSearchDto.builder().minPrice(20000).maxPrice(20000).build()::validate);
	}

	@Test
	void negativePrice_rejected() {
		assertThrows(IllegalArgumentException.class, GameSearchDto.builder().minPrice(-1).build()::validate);
	}

	@Test
	void person_zero_rejected() {
		assertThrows(IllegalArgumentException.class, GameSearchDto.builder().person(0).build()::validate);
	}

	@Test
	void near_partial_rejected() {
		assertThrows(IllegalArgumentException.class, GameSearchDto.builder().lat(37.5).lng(127.0).build()::validate);
		assertThrows(IllegalArgumentException.class, GameSearchDto.builder().radiusKm(3.0).build()::validate);
	}

	@Test
	void near_radiusOutOfRange_rejected() {
		assertThrows(IllegalArgumentException.class,
			GameSearchDto.builder().lat(37.5).lng(127.0).radiusKm(0.0).build()::validate);
		assertThrows(IllegalArgumentException.class,
			GameSearchDto.builder().lat(37.5).lng(127.0).radiusKm(50.1).build()::validate);
		assertDoesNotThrow(GameSearchDto.builder().lat(37.5).lng(127.0).radiusKm(50.0).build()::validate);
	}

	@Test
	void near_badCoordinates_rejected() {
		assertThrows(IllegalArgumentException.class,
			GameSearchDto.builder().lat(91.0).lng(127.0).radiusKm(3.0).build()::validate);
		assertThrows(IllegalArgumentException.class,
			GameSearchDto.builder().lat(37.5).lng(-181.0).radiusKm(3.0).build()::validate);
	}

	@Test
	void distanceSort_withoutNear_rejected() {
		assertThrows(IllegalArgumentException.class,
			GameSearchDto.builder().sort(GameSortType.DISTANCE).build()::validate);
		assertDoesNotThrow(GameSearchDto.builder().sort(GameSortType.DISTANCE)
			.lat(37.5).lng(127.0).radiusKm(3.0).build()::validate);
	}
}
