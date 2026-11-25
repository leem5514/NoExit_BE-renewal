package com.E1i3.NoExit.domain.game.service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.E1i3.NoExit.domain.game.domain.GameTimeSlot;
import com.E1i3.NoExit.domain.game.domain.GameTimeSlotPolicy;
import com.E1i3.NoExit.domain.game.dto.GameDetailResDto;
import com.E1i3.NoExit.domain.game.dto.GameSearchDto;
import com.E1i3.NoExit.domain.game.dto.GameSearchResDto;
import com.E1i3.NoExit.domain.game.dto.GameSortType;
import com.E1i3.NoExit.domain.game.repository.GameSpecifications;
import com.E1i3.NoExit.domain.game.repository.GameTimeSlotRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

	// 복합 조건 검색. 근처 검색이 없으면 DB 정렬·페이징, 있으면 바운딩 박스로 후보를 가져와
	// 메모리에서 정확한 반경 판정·정렬·페이징 (docs/plans/search-filters.md).
	public Page<GameSearchResDto> search(GameSearchDto dto, int page, int size) {
		dto.validate();
		if (page < 0) {
			throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("size 는 1 ~ " + MAX_PAGE_SIZE + " 이어야 합니다.");
		}
		GameSortType sortType = dto.sortOrDefault();
		Specification<Game> spec = GameSpecifications.of(dto);

		if (!dto.hasNear()) {
			Pageable pageable = PageRequest.of(page, size, dbSort(sortType));
			return gameRepository.findAll(spec, pageable).map(g -> GameSearchResDto.of(g, null));
		}

		List<GameWithDistance> hits = new ArrayList<>();
		for (Game game : gameRepository.findAll(spec)) {
			Store store = game.getStore();
			if (store.getLatitude() == null || store.getLongitude() == null) {
				continue;
			}
			double d = GeoUtils.distanceKm(dto.getLat(), dto.getLng(), store.getLatitude(), store.getLongitude());
			if (d <= dto.getRadiusKm()) {
				hits.add(new GameWithDistance(game, d));
			}
		}
		hits.sort(memorySort(sortType));

		int from = (int) Math.min((long) page * size, hits.size());
		int to = Math.min(from + size, hits.size());
		List<GameSearchResDto> content = new ArrayList<>();
		for (GameWithDistance h : hits.subList(from, to)) {
			content.add(GameSearchResDto.of(h.game, h.distanceKm));
		}
		return new PageImpl<>(content, PageRequest.of(page, size), hits.size());
	}

	static final int MAX_PAGE_SIZE = 100;

	private static Sort dbSort(GameSortType sortType) {
		switch (sortType) {
			case PRICE_ASC:
				return Sort.by(Sort.Order.asc("price"), Sort.Order.asc("id"));
			case PRICE_DESC:
				return Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
			case LATEST:
			default: // DISTANCE 는 validate 에서 근처 검색 없이 오지 못한다
				return Sort.by(Sort.Order.desc("createdTime"), Sort.Order.desc("id"));
		}
	}

	private static Comparator<GameWithDistance> memorySort(GameSortType sortType) {
		Comparator<GameWithDistance> byId = Comparator.comparing(h -> h.game.getId(),
			Comparator.nullsLast(Comparator.naturalOrder()));
		switch (sortType) {
			case DISTANCE:
				return Comparator.<GameWithDistance>comparingDouble(h -> h.distanceKm).thenComparing(byId);
			case PRICE_ASC:
				return Comparator.<GameWithDistance>comparingInt(h -> h.game.getPrice()).thenComparing(byId);
			case PRICE_DESC:
				return Comparator.<GameWithDistance>comparingInt(h -> h.game.getPrice()).reversed().thenComparing(byId);
			case LATEST:
			default:
				Comparator<GameWithDistance> byCreated = Comparator.comparing(h -> h.game.getCreatedTime(),
					Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));
				return byCreated.thenComparing(byId.reversed());
		}
	}

	private static final class GameWithDistance {
		final Game game;
		final double distanceKm;

		GameWithDistance(Game game, double distanceKm) {
			this.game = game;
			this.distanceKm = distanceKm;
		}
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
