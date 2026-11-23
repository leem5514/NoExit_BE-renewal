package com.E1i3.NoExit.domain.store.service;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreResDto;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreSaveReqDto;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;

// 사장님 본인 매장 등록·수정·목록. 사장님은 항상 토큰(SecurityContext)에서 결정한다.
@Service
@Transactional
public class OwnerStoreService {

	private final StoreRepository storeRepository;
	private final OwnerRepository ownerRepository;

	public OwnerStoreService(StoreRepository storeRepository, OwnerRepository ownerRepository) {
		this.storeRepository = storeRepository;
		this.ownerRepository = ownerRepository;
	}

	public OwnerStoreResDto create(OwnerStoreSaveReqDto dto) {
		Owner owner = currentOwner();
		Store saved = storeRepository.save(dto.toEntity(owner));
		return OwnerStoreResDto.from(saved);
	}

	public OwnerStoreResDto update(Long storeId, OwnerStoreSaveReqDto dto) {
		Owner owner = currentOwner();
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));
		checkStoreOwner(store, owner, "본인 매장만 수정할 수 있습니다.");
		store.update(dto);
		return OwnerStoreResDto.from(store);
	}

	@Transactional(readOnly = true)
	public List<OwnerStoreResDto> myStores() {
		Owner owner = currentOwner();
		return storeRepository.findByOwner(owner).stream()
			.map(OwnerStoreResDto::from)
			.collect(Collectors.toList());
	}

	private Owner currentOwner() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return ownerRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("등록되지 않은 점장 이메일입니다."));
	}

	// 게임 서비스에서도 같은 규칙을 쓰도록 공개한다.
	public static void checkStoreOwner(Store store, Owner owner, String message) {
		Owner storeOwner = store.getOwner();
		if (storeOwner == null || !storeOwner.getId().equals(owner.getId())) {
			throw new IllegalArgumentException(message);
		}
	}
}
