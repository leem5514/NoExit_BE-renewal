package com.E1i3.NoExit.domain.store.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.E1i3.NoExit.domain.common.dto.CommonResDto;
import com.E1i3.NoExit.domain.game.dto.GameTimeSlotReqDto;
import com.E1i3.NoExit.domain.game.dto.GameTimeSlotResDto;
import com.E1i3.NoExit.domain.game.dto.OwnerGameResDto;
import com.E1i3.NoExit.domain.game.dto.OwnerGameSaveReqDto;
import com.E1i3.NoExit.domain.game.service.OwnerGameService;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreResDto;
import com.E1i3.NoExit.domain.store.dto.OwnerStoreSaveReqDto;
import com.E1i3.NoExit.domain.store.service.OwnerStoreService;

// 사장님 본인 매장·게임 등록/수정. URL 레벨(SecurityConfigs)과 메서드 레벨 모두 OWNER 만 허용.
@RestController
@PreAuthorize("hasRole('OWNER')")
public class OwnerStoreController {

	private final OwnerStoreService ownerStoreService;
	private final OwnerGameService ownerGameService;

	public OwnerStoreController(OwnerStoreService ownerStoreService, OwnerGameService ownerGameService) {
		this.ownerStoreService = ownerStoreService;
		this.ownerGameService = ownerGameService;
	}

	@PostMapping("/owner/stores")
	public ResponseEntity<?> createStore(@Valid @RequestBody OwnerStoreSaveReqDto dto) {
		OwnerStoreResDto store = ownerStoreService.create(dto);
		return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "매장이 등록되었습니다.", store), HttpStatus.CREATED);
	}

	@PutMapping("/owner/stores/{storeId}")
	public ResponseEntity<?> updateStore(@PathVariable Long storeId, @Valid @RequestBody OwnerStoreSaveReqDto dto) {
		OwnerStoreResDto store = ownerStoreService.update(storeId, dto);
		return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "매장이 수정되었습니다.", store), HttpStatus.OK);
	}

	@GetMapping("/owner/stores")
	public ResponseEntity<?> myStores() {
		List<OwnerStoreResDto> stores = ownerStoreService.myStores();
		return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "OK", stores), HttpStatus.OK);
	}

	@PostMapping("/owner/stores/{storeId}/games")
	public ResponseEntity<?> createGame(@PathVariable Long storeId, @Valid @RequestBody OwnerGameSaveReqDto dto) {
		OwnerGameResDto game = ownerGameService.create(storeId, dto);
		return new ResponseEntity<>(new CommonResDto(HttpStatus.CREATED, "게임이 등록되었습니다.", game), HttpStatus.CREATED);
	}

	@PutMapping("/owner/games/{gameId}")
	public ResponseEntity<?> updateGame(@PathVariable Long gameId, @Valid @RequestBody OwnerGameSaveReqDto dto) {
		OwnerGameResDto game = ownerGameService.update(gameId, dto);
		return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "게임이 수정되었습니다.", game), HttpStatus.OK);
	}

	@PutMapping("/owner/games/{gameId}/time-slots")
	public ResponseEntity<?> replaceTimeSlots(@PathVariable Long gameId, @Valid @RequestBody GameTimeSlotReqDto dto) {
		GameTimeSlotResDto slots = ownerGameService.replaceTimeSlots(gameId, dto.getStartTimes());
		return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "예약 시간대가 저장되었습니다.", slots), HttpStatus.OK);
	}
}
