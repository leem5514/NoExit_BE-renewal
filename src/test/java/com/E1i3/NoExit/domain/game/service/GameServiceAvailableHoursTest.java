package com.E1i3.NoExit.domain.game.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.domain.GameTimeSlot;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.game.repository.GameTimeSlotRepository;
import com.E1i3.NoExit.domain.store.domain.Store;

@ExtendWith(MockitoExtension.class)
class GameServiceAvailableHoursTest {

	@Mock private GameRepository gameRepository;
	@Mock private GameTimeSlotRepository gameTimeSlotRepository;

	private GameService gameService;
	private Game game;

	@BeforeEach
	void setUp() {
		gameService = new GameService(gameRepository, gameTimeSlotRepository);
		Store store = Store.builder().id(10L).storeName("S").openingHours("10:00 - 13:00").build();
		game = Game.builder().id(100L).gameName("G").runningTime("60분").store(store).build();
		when(gameRepository.findById(100L)).thenReturn(Optional.of(game));
	}

	@Test
	void custom_slots_are_used_when_present() {
		when(gameTimeSlotRepository.findByGameIdOrderByStartTimeAsc(100L)).thenReturn(List.of(
			new GameTimeSlot(game, LocalTime.of(10, 30)), new GameTimeSlot(game, LocalTime.of(12, 0))));

		assertEquals(List.of(LocalTime.of(10, 30), LocalTime.of(12, 0)), gameService.getAvailableHours(100L));
	}

	@Test
	void hourly_slots_are_used_when_no_custom_slots() {
		when(gameTimeSlotRepository.findByGameIdOrderByStartTimeAsc(100L)).thenReturn(List.of());

		assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0), LocalTime.of(13, 0)),
			gameService.getAvailableHours(100L));
	}
}
