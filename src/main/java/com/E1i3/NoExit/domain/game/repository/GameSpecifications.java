package com.E1i3.NoExit.domain.game.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.dto.GameSearchDto;
import com.E1i3.NoExit.domain.game.service.GeoUtils;
import com.E1i3.NoExit.domain.store.domain.Store;

public final class GameSpecifications {

	private GameSpecifications() {
	}

	// 검색 조건을 AND 로 조합. 근처 검색이면 반경을 감싸는 바운딩 박스까지만 DB에서 거른다
	// (정확한 반경 판정·거리 정렬은 GameService 에서 메모리로).
	public static Specification<Game> of(GameSearchDto dto) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			Join<Game, Store> store = root.join("store", JoinType.INNER);

			if (hasText(dto.getGameName())) {
				predicates.add(cb.like(root.get("gameName"), "%" + escape(dto.getGameName().trim()) + "%", ESCAPE));
			}
			if (hasText(dto.getRegion())) {
				predicates.add(cb.like(store.get("address"), "%" + escape(dto.getRegion().trim()) + "%", ESCAPE));
			}
			if (dto.getDifficult() != null && !dto.getDifficult().isEmpty()) {
				predicates.add(root.get("difficult").in(dto.getDifficult()));
			}
			if (dto.getMinPrice() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("price"), dto.getMinPrice()));
			}
			if (dto.getMaxPrice() != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("price"), dto.getMaxPrice()));
			}
			if (dto.getPerson() != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("maximunPerson"), dto.getPerson()));
			}
			if (dto.hasNear()) {
				GeoUtils.BoundingBox box = GeoUtils.boundingBox(dto.getLat(), dto.getLng(), dto.getRadiusKm());
				// BETWEEN 은 좌표가 null 인 매장을 자연히 제외한다.
				predicates.add(cb.between(store.get("latitude"), box.minLat, box.maxLat));
				predicates.add(cb.between(store.get("longitude"), box.minLng, box.maxLng));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private static boolean hasText(String s) {
		return s != null && !s.trim().isEmpty();
	}

	// MariaDB 는 문자열 리터럴 안의 '\' 를 이스케이프로 해석하므로 ESCAPE 문자로 '!' 를 쓴다.
	static final char ESCAPE = '!';

	// LIKE 와일드카드(%, _)를 문자 그대로 검색
	static String escape(String s) {
		return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
	}
}
