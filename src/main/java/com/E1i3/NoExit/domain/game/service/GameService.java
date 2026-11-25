package com.E1i3.NoExit.domain.game.service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.E1i3.NoExit.domain.game.domain.GameTimeSlot;
import com.E1i3.NoExit.domain.game.domain.GameTimeSlotPolicy;
import com.E1i3.NoExit.domain.game.dto.GameDetailResDto;
import com.E1i3.NoExit.domain.game.repository.GameTimeSlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.dto.GameResDto;
import com.E1i3.NoExit.domain.game.repository.GameRepository;

import javax.persistence.EntityNotFoundException;

@Service
@Transactional(readOnly = true)
public class GameService {
	private final GameRepository gameRepository;
	private final GameTimeSlotRepository gameTimeSlotRepository;

	@Autowired
	public GameService(GameRepository gameRepository, GameTimeSlotRepository gameTimeSlotRepository) {
		this.gameRepository = gameRepository;
		this.gameTimeSlotRepository = gameTimeSlotRepository;
	}

	@Transactional(readOnly = true)
	public Page<GameResDto> gameList(Pageable pageable) {
		Page<Game> gameList = gameRepository.findAll(pageable);
		return gameList.map(Game::fromEntity);
	}

	@Transactional
	public GameDetailResDto getGameDetail(Long gameId) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new EntityNotFoundException("게임을 찾을 수 없습니다."));
		return GameDetailResDto.fromEntity(game);
	}

	// 게임 예약 가능 시작 시각: 사장님이 지정한 시간대가 있으면 그것, 없으면 매장 영업시간을 1시간 단위로.
	public List<LocalTime> getAvailableHours(Long gameId) {
		Game game = gameRepository.findById(gameId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid game ID"));
		return getAvailableHours(game);
	}

	public List<LocalTime> getAvailableHours(Game game) {
		List<GameTimeSlot> slots = gameTimeSlotRepository.findByGameIdOrderByStartTimeAsc(game.getId());
		if (!slots.isEmpty()) {
			return slots.stream().map(GameTimeSlot::getStartTime).collect(Collectors.toList());
		}
		return GameTimeSlotPolicy.hourlySlots(game.getStore().getOpeningHours());
	}

	public List<GameResDto> gameListAll() {
		List<Game> gameList = gameRepository.findAll();
		List<GameResDto> gameResDtolist = new ArrayList<>();
		for (Game game : gameList) {
			gameResDtolist.add(game.fromEntity());
		}
		return gameResDtolist;
	}
}
