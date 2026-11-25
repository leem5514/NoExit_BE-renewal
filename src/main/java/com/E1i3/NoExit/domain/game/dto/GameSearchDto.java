package com.E1i3.NoExit.domain.game.dto;

import java.util.List;

import com.E1i3.NoExit.domain.game.domain.Difficult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// GET /game/search 쿼리 파라미터. 모든 필터는 선택 사항이며 null/빈 값이면 적용하지 않는다.
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GameSearchDto {

    public static final double MAX_RADIUS_KM = 50.0;

    private String gameName;          // 게임명 부분 일치
    private String region;            // 매장 주소 부분 일치
    private List<Difficult> difficult; // 난이도 (여러 개, IN)
    private Integer minPrice;
    private Integer maxPrice;
    private Integer person;           // maximunPerson >= person

    private Double lat;
    private Double lng;
    private Double radiusKm;

    private GameSortType sort;        // null 이면 LATEST

    public boolean hasNear() {
        return lat != null && lng != null && radiusKm != null;
    }

    public GameSortType sortOrDefault() {
        return sort != null ? sort : GameSortType.LATEST;
    }

    // 잘못된 조합은 IllegalArgumentException (→ 400)
    public void validate() {
        if (minPrice != null && minPrice < 0) {
            throw new IllegalArgumentException("minPrice 는 0 이상이어야 합니다.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice 는 0 이상이어야 합니다.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new IllegalArgumentException("minPrice 는 maxPrice 보다 클 수 없습니다.");
        }
        if (person != null && person < 1) {
            throw new IllegalArgumentException("person 은 1 이상이어야 합니다.");
        }
        int nearCount = (lat != null ? 1 : 0) + (lng != null ? 1 : 0) + (radiusKm != null ? 1 : 0);
        if (nearCount != 0 && nearCount != 3) {
            throw new IllegalArgumentException("근처 검색은 lat, lng, radiusKm 를 모두 지정해야 합니다.");
        }
        if (hasNear()) {
            if (lat < -90 || lat > 90) {
                throw new IllegalArgumentException("lat 은 -90 ~ 90 이어야 합니다.");
            }
            if (lng < -180 || lng > 180) {
                throw new IllegalArgumentException("lng 는 -180 ~ 180 이어야 합니다.");
            }
            if (radiusKm <= 0 || radiusKm > MAX_RADIUS_KM) {
                throw new IllegalArgumentException("radiusKm 는 0 초과 " + (int) MAX_RADIUS_KM + " 이하여야 합니다.");
            }
        }
        if (sortOrDefault() == GameSortType.DISTANCE && !hasNear()) {
            throw new IllegalArgumentException("거리순 정렬은 lat, lng, radiusKm 가 필요합니다.");
        }
    }
}
