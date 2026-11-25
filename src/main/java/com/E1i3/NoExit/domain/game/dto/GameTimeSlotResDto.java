package com.E1i3.NoExit.domain.game.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import com.E1i3.NoExit.domain.game.domain.GameTimeSlotPolicy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GameTimeSlotResDto {
	private Long gameId;
	private List<String> startTimes; // 정렬된 "HH:mm". 비어 있으면 1시간 단위 방식 사용

	public static GameTimeSlotResDto of(Long gameId, List<LocalTime> startTimes) {
		return new GameTimeSlotResDto(gameId,
			startTimes.stream().map(t -> t.format(GameTimeSlotPolicy.HH_MM)).collect(Collectors.toList()));
	}
}
