package com.E1i3.NoExit.domain.game.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.E1i3.NoExit.domain.game.domain.AgeLimit;
import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.dto.OwnerGameResDto;
import com.E1i3.NoExit.domain.game.dto.OwnerGameSaveReqDto;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class OwnerGameServiceTest {

	@Mock private GameRepository gameRepository;
	@Mock private StoreRepository storeRepository;
	@Mock private OwnerRepository ownerRepository;

	private OwnerGameService ownerGameService;

	private final Owner myOwner = Owner.builder().id(1L).email("owner@test.com").storeName("My Store").build();
	private final Owner otherOwner = Owner.builder().id(2L).email("other@test.com").storeName("Other Store").build();

	@BeforeEach
	void setUp() {
		ownerGameService = new OwnerGameService(gameRepository, storeRepository, ownerRepository);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("owner@test.com", null));
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private OwnerGameSaveReqDto request(String name) {
		return OwnerGameSaveReqDto.builder()
			.gameName(name).runningTime("60분").difficult(Difficult.THREE)
			.price(25000).maximunPerson(4).ageLimit(AgeLimit.STUDENT).gameInfo("설명")
			.build();
	}

	private Store storeOf(Owner owner) {
		return Store.builder().id(10L).storeName(owner.getStoreName()).openingHours("10:00 - 22:00")
			.owner(owner).build();
	}

	private Game gameOf(Store store) {
		return Game.builder().id(100L).gameName("Old").runningTime("50분").difficult(Difficult.ONE)
			.price(10000).maximunPerson(2).ageLimit(AgeLimit.ADULT)
			.wishCount(7).imagePath("https://img/old.png").store(store).build();
	}

	@Test
	void create_on_own_store_links_store_with_zero_wish_and_no_image() {
		Store store = storeOf(myOwner);
		when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
		when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

		OwnerGameResDto result = ownerGameService.create(10L, request("Escape"));

		ArgumentCaptor<Game> captor = ArgumentCaptor.forClass(Game.class);
		verify(gameRepository).save(captor.capture());
		Game saved = captor.getValue();
		assertSame(store, saved.getStore());
		assertEquals(0, saved.getWishCount());
		assertNull(saved.getImagePath());
		assertEquals(4, saved.getMaximunPerson());
		assertTrue(store.getGames().contains(saved));
		assertEquals(10L, result.getStoreId());
		assertEquals("Escape", result.getGameName());
	}

	@Test
	void create_on_other_owners_store_is_rejected() {
		Store store = storeOf(otherOwner);
		when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

		assertThrows(IllegalArgumentException.class, () -> ownerGameService.create(10L, request("Escape")));
		verify(gameRepository, never()).save(any());
		assertTrue(store.getGames().isEmpty());
	}

	@Test
	void create_on_missing_store_throws_entity_not_found() {
		when(storeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> ownerGameService.create(99L, request("Escape")));
		verify(gameRepository, never()).save(any());
	}

	@Test
	void update_own_game_applies_fields_and_keeps_store_wish_and_image() {
		Store store = storeOf(myOwner);
		Game game = gameOf(store);
		when(gameRepository.findById(100L)).thenReturn(Optional.of(game));

		OwnerGameResDto result = ownerGameService.update(100L, request("Renamed"));

		assertEquals("Renamed", game.getGameName());
		assertEquals("60분", game.getRunningTime());
		assertEquals(Difficult.THREE, game.getDifficult());
		assertEquals(25000, game.getPrice());
		assertEquals(4, game.getMaximunPerson());
		assertEquals(AgeLimit.STUDENT, game.getAgeLimit());
		assertSame(store, game.getStore());
		assertEquals(7, game.getWishCount());
		assertEquals("https://img/old.png", game.getImagePath());
		assertEquals("Renamed", result.getGameName());
	}

	@Test
	void update_other_owners_game_is_rejected_without_changes() {
		Game game = gameOf(storeOf(otherOwner));
		when(gameRepository.findById(100L)).thenReturn(Optional.of(game));

		assertThrows(IllegalArgumentException.class, () -> ownerGameService.update(100L, request("Hijacked")));
		assertEquals("Old", game.getGameName());
		assertEquals(10000, game.getPrice());
	}

	@Test
	void update_missing_game_throws_entity_not_found() {
		when(gameRepository.findById(999L)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> ownerGameService.update(999L, request("X")));
	}
}
