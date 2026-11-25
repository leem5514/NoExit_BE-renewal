package com.E1i3.NoExit.domain.game.dto;

import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.store.domain.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameSearchResDto {
	private Long id;
	private String gameName;
	private Difficult difficult;
	private int price;
	private int maximunPerson;
	private String imagePath;
	private Long storeId;
	private String storeName;
	private String storeAddress;
	private Double distanceKm; // 근처 검색일 때만 값이 있다 (소수 둘째 자리 반올림)

	public static GameSearchResDto of(Game game, Double distanceKm) {
		Store store = game.getStore();
		return GameSearchResDto.builder()
			.id(game.getId())
			.gameName(game.getGameName())
			.difficult(game.getDifficult())
			.price(game.getPrice())
			.maximunPerson(game.getMaximunPerson())
			.imagePath(game.getImagePath())
			.storeId(store.getId())
			.storeName(store.getStoreName())
			.storeAddress(store.getAddress())
			.distanceKm(distanceKm == null ? null : Math.round(distanceKm * 100) / 100.0)
			.build();
	}
}
