package com.E1i3.NoExit.domain.game.service;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.domain.GameTimeSlot;
import com.E1i3.NoExit.domain.game.domain.GameTimeSlotPolicy;
import com.E1i3.NoExit.domain.game.dto.GameTimeSlotResDto;
import com.E1i3.NoExit.domain.game.dto.OwnerGameResDto;
import com.E1i3.NoExit.domain.game.dto.OwnerGameSaveReqDto;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.game.repository.GameTimeSlotRepository;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;
import com.E1i3.NoExit.domain.store.service.OwnerStoreService;

// 사장님 본인 매장의 게임(테마) 등록·수정. 이미지는 이번 범위에서 제외 (imagePath 등록 시 null, 수정 시 유지).
@Service
@Transactional
public class OwnerGameService {

	private final GameRepository gameRepository;
	private final StoreRepository storeRepository;
	private final OwnerRepository ownerRepository;

	private final GameTimeSlotRepository gameTimeSlotRepository;

	public OwnerGameService(GameRepository gameRepository, StoreRepository storeRepository,
		OwnerRepository ownerRepository, GameTimeSlotRepository gameTimeSlotRepository) {
		this.gameRepository = gameRepository;
		this.storeRepository = storeRepository;
		this.ownerRepository = ownerRepository;
		this.gameTimeSlotRepository = gameTimeSlotRepository;
	}

	public OwnerGameResDto create(Long storeId, OwnerGameSaveReqDto dto) {
		Owner owner = currentOwner();
		Store store = storeRepository.findById(storeId)
			.orElseThrow(() -> new EntityNotFoundException("매장을 찾을 수 없습니다."));
		OwnerStoreService.checkStoreOwner(store, owner, "본인 매장에만 게임을 등록할 수 있습니다.");

		Game game = dto.toEntity(store);
		store.addGame(game);
		Game saved = gameRepository.save(game);
		return OwnerGameResDto.from(saved);
	}

	public OwnerGameResDto update(Long gameId, OwnerGameSaveReqDto dto) {
		Owner owner = currentOwner();
		Game game = gameRepository.findById(gameId)
			.orElseThrow(() -> new EntityNotFoundException("게임을 찾을 수 없습니다."));
		OwnerStoreService.checkStoreOwner(game.getStore(), owner, "본인 매장의 게임만 수정할 수 있습니다.");
		game.update(dto);
		return OwnerGameResDto.from(game);
	}

	// 예약 시작 시각 전체 교체. 빈 목록이면 사용자 지정 해제(1시간 단위 방식으로 복귀).
	public GameTimeSlotResDto replaceTimeSlots(Long gameId, List<String> startTimes) {
		Owner owner = currentOwner();
		Game game = gameRepository.findById(gameId)
			.orElseThrow(() -> new EntityNotFoundException("게임을 찾을 수 없습니다."));
		OwnerStoreService.checkStoreOwner(game.getStore(), owner, "본인 매장의 게임만 시간대를 설정할 수 있습니다.");

		List<LocalTime> slots = GameTimeSlotPolicy.validate(
			startTimes, game.getStore().getOpeningHours(), game.getRunningTime());

		gameTimeSlotRepository.deleteAllByGameId(game.getId());
		gameTimeSlotRepository.saveAll(
			slots.stream().map(t -> new GameTimeSlot(game, t)).collect(Collectors.toList()));
		return GameTimeSlotResDto.of(game.getId(), slots);
	}

	private Owner currentOwner() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		return ownerRepository.findByEmail(email)
			.orElseThrow(() -> new IllegalArgumentException("등록되지 않은 점장 이메일입니다."));
	}
}
