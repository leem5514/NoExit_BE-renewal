package com.E1i3.NoExit.domain.game.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.E1i3.NoExit.domain.game.domain.AgeLimit;
import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.store.domain.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사장님 게임 등록·수정 공용 요청 DTO. store·wishCount·imagePath 는 받지 않는다.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerGameSaveReqDto {

	@NotBlank(message = "게임명은 필수입니다.")
	@Size(max = 255, message = "게임명은 255자 이하여야 합니다.")
	private String gameName;

	@NotBlank(message = "러닝타임은 필수입니다.")
	private String runningTime;

	@NotNull(message = "난이도는 필수입니다.")
	private Difficult difficult;

	@NotNull(message = "가격은 필수입니다.")
	@Min(value = 0, message = "가격은 0 이상이어야 합니다.")
	private Integer price;

	@NotNull(message = "최대 인원은 필수입니다.")
	@Min(value = 1, message = "최대 인원은 1 이상이어야 합니다.")
	private Integer maximunPerson; // 엔티티 철자 그대로

	@NotNull(message = "나이 제한은 필수입니다.")
	private AgeLimit ageLimit;

	private String gameInfo;

	public Game toEntity(Store store) {
		return Game.builder()
			.gameName(gameName)
			.runningTime(runningTime)
			.difficult(difficult)
			.price(price)
			.maximunPerson(maximunPerson)
			.ageLimit(ageLimit)
			.gameInfo(gameInfo)
			.wishCount(0)
			.imagePath(null)
			.store(store)
			.build();
	}
}
