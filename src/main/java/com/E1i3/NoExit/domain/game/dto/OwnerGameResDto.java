package com.E1i3.NoExit.domain.game.dto;

import com.E1i3.NoExit.domain.game.domain.AgeLimit;
import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerGameResDto {
	private Long id;
	private Long storeId;
	private String gameName;
	private String runningTime;
	private Difficult difficult;
	private int price;
	private int maximunPerson;
	private AgeLimit ageLimit;
	private String gameInfo;
	private int wishCount;
	private String imagePath;

	public static OwnerGameResDto from(Game game) {
		return OwnerGameResDto.builder()
			.id(game.getId())
			.storeId(game.getStore().getId())
			.gameName(game.getGameName())
			.runningTime(game.getRunningTime())
			.difficult(game.getDifficult())
			.price(game.getPrice())
			.maximunPerson(game.getMaximunPerson())
			.ageLimit(game.getAgeLimit())
			.gameInfo(game.getGameInfo())
			.wishCount(game.getWishCount())
			.imagePath(game.getImagePath())
			.build();
	}
}
