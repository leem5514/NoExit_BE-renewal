package com.E1i3.NoExit.domain.game.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 게임 예약 시작 시각 전체 교체 요청. 빈 배열이면 사용자 지정 해제(1시간 단위 방식으로 복귀).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameTimeSlotReqDto {

	@NotNull(message = "시작 시각 목록(startTimes)은 필수입니다.")
	private List<String> startTimes; // "HH:mm"
}
