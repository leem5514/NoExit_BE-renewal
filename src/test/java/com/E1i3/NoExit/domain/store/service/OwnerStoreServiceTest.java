package com.E1i3.NoExit.domain.store.service;

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

import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.store.domain.OpenStatus;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreResDto;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreSaveReqDto;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;

@ExtendWith(MockitoExtension.class)
class OwnerStoreServiceTest {

	@Mock private StoreRepository storeRepository;
	@Mock private OwnerRepository ownerRepository;

	private OwnerStoreService ownerStoreService;

	private final Owner myOwner = Owner.builder().id(1L).email("owner@test.com").storeName("My Store").build();
	private final Owner otherOwner = Owner.builder().id(2L).email("other@test.com").storeName("Other Store").build();

	@BeforeEach
	void setUp() {
		ownerStoreService = new OwnerStoreService(storeRepository, ownerRepository);
		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken("owner@test.com", null));
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private OwnerStoreSaveReqDto request(String name, String hours) {
		return OwnerStoreSaveReqDto.builder()
			.storeName(name).openingHours(hours)
			.phoneNumber("02-000-0000").address("서울").info("소개")
			.latitude(37.5).longitude(127.0)
			.build();
	}

	private Store storeOf(Owner owner) {
		return Store.builder().id(10L).storeName("Old").storeRating(4).openingHours("09:00 - 18:00")
			.openStatus(OpenStatus.DAYOFF).owner(owner).build();
	}

	@Test
	void create_saves_store_with_token_owner_rating_zero_and_default_open() {
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
		when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));

		OwnerStoreResDto result = ownerStoreService.create(request("New Store", "10:00 - 22:00"));

		ArgumentCaptor<Store> captor = ArgumentCaptor.forClass(Store.class);
		verify(storeRepository).save(captor.capture());
		Store saved = captor.getValue();
		assertSame(myOwner, saved.getOwner());
		assertEquals(0, saved.getStoreRating());
		assertEquals(OpenStatus.OPEN, saved.getOpenStatus());
		assertEquals("10:00 - 22:00", saved.getOpeningHours());
		assertNotNull(saved.getGames());
		assertEquals("New Store", result.getStoreName());
		assertEquals(1L, result.getOwnerId());
	}

	@Test
	void create_rejects_unregistered_owner_email() {
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
			() -> ownerStoreService.create(request("New Store", "10:00 - 22:00")));
		verify(storeRepository, never()).save(any());
	}

	@Test
	void update_own_store_applies_fields_and_keeps_owner_and_rating() {
		Store store = storeOf(myOwner);
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
		when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

		OwnerStoreSaveReqDto dto = request("Renamed", "11:00 - 23:00");
		dto.setOpenStatus(OpenStatus.CLOSED);
		OwnerStoreResDto result = ownerStoreService.update(10L, dto);

		assertEquals("Renamed", store.getStoreName());
		assertEquals("11:00 - 23:00", store.getOpeningHours());
		assertEquals(OpenStatus.CLOSED, store.getOpenStatus());
		assertEquals("서울", store.getAddress());
		assertSame(myOwner, store.getOwner());
		assertEquals(4, store.getStoreRating());
		assertEquals("Renamed", result.getStoreName());
	}

	@Test
	void update_rejects_other_owners_store_without_changes() {
		Store store = storeOf(otherOwner);
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
		when(storeRepository.findById(10L)).thenReturn(Optional.of(store));

		assertThrows(IllegalArgumentException.class,
			() -> ownerStoreService.update(10L, request("Hijacked", "10:00 - 22:00")));

		assertEquals("Old", store.getStoreName());
		assertEquals("09:00 - 18:00", store.getOpeningHours());
		assertSame(otherOwner, store.getOwner());
	}

	@Test
	void update_missing_store_throws_entity_not_found() {
		when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
		when(storeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class,
			() -> ownerStoreService.update(99L, request("X", "10:00 - 22:00")));
	}
}
