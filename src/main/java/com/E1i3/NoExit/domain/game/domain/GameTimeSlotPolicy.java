package com.E1i3.NoExit.domain.game.domain;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 게임 예약 시간대 규칙 (순수 로직). 위반 시 IllegalArgumentException → 400.
public final class GameTimeSlotPolicy {

	public static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	private static final Pattern HH_MM_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
	private static final Pattern OPENING_HOURS = Pattern.compile("^(\\d{2}:\\d{2}) - (\\d{2}:\\d{2})$");
	private static final Pattern MINUTES_ONLY = Pattern.compile("^(\\d+)\\s*(분|min)?$");
	private static final Pattern HOURS_MINUTES = Pattern.compile("^(\\d+)\\s*시간(?:\\s*(\\d+)\\s*분)?$");

	private GameTimeSlotPolicy() {
	}

	/** 사장님이 보낸 시작 시각 목록을 검증하고 정렬된 LocalTime 목록으로 돌려준다. */
	public static List<LocalTime> validate(List<String> startTimes, String openingHours, String runningTime) {
		if (startTimes == null) {
			throw new IllegalArgumentException("시작 시각 목록은 필수입니다.");
		}
		if (startTimes.isEmpty()) {
			return new ArrayList<>();
		}
		LocalTime[] hours = parseOpeningHours(openingHours);
		int open = minuteOfDay(hours[0]);
		int close = minuteOfDay(hours[1]);
		int running = runningMinutes(runningTime);

		Set<LocalTime> seen = new HashSet<>();
		List<LocalTime> result = new ArrayList<>();
		for (String raw : startTimes) {
			if (raw == null || !HH_MM_PATTERN.matcher(raw).matches()) {
				throw new IllegalArgumentException("시작 시각은 HH:mm 형식이어야 합니다: " + raw);
			}
			LocalTime start = LocalTime.parse(raw, HH_MM);
			if (!seen.add(start)) {
				throw new IllegalArgumentException("중복된 시작 시각이 있습니다: " + raw);
			}
			int s = minuteOfDay(start);
			if (s < open || s >= close) {
				throw new IllegalArgumentException("시작 시각이 영업시간(" + openingHours + ") 밖입니다: " + raw);
			}
			if (s + running > close) {
				throw new IllegalArgumentException(
					"시작 시각 " + raw + " + 진행시간 " + running + "분이 영업 종료(" + hours[1].format(HH_MM) + ")를 넘습니다.");
			}
			result.add(start);
		}
		result.sort(null);
		return result;
	}

	/** 기존 방식: 영업 시작부터 종료까지(종료 포함) 1시간 간격. 동작을 바꾸지 않기 위해 그대로 옮겼다. */
	public static List<LocalTime> hourlySlots(String openingHours) {
		String[] times = openingHours.split(" - ");
		LocalTime startTime = LocalTime.parse(times[0]);
		LocalTime endTime = LocalTime.parse(times[1]);

		List<LocalTime> availableHours = new ArrayList<>();
		while (!startTime.isAfter(endTime)) {
			availableHours.add(startTime);
			startTime = startTime.plusHours(1);
		}
		return availableHours;
	}

	/** "60", "60분", "60min", "1시간", "1시간 30분" → 분. 그 외 형식은 거부. */
	public static int runningMinutes(String runningTime) {
		String value = runningTime == null ? "" : runningTime.trim();
		int minutes;
		Matcher m = MINUTES_ONLY.matcher(value);
		Matcher hm = HOURS_MINUTES.matcher(value);
		if (m.matches()) {
			minutes = Integer.parseInt(m.group(1));
		} else if (hm.matches()) {
			minutes = Integer.parseInt(hm.group(1)) * 60 + (hm.group(2) != null ? Integer.parseInt(hm.group(2)) : 0);
		} else {
			throw new IllegalArgumentException("게임 진행시간을 분 단위로 해석할 수 없습니다: " + runningTime);
		}
		if (minutes <= 0) {
			throw new IllegalArgumentException("게임 진행시간은 1분 이상이어야 합니다: " + runningTime);
		}
		return minutes;
	}

	private static LocalTime[] parseOpeningHours(String openingHours) {
		Matcher m = openingHours == null ? null : OPENING_HOURS.matcher(openingHours);
		if (m == null || !m.matches()) {
			throw new IllegalArgumentException("매장 영업시간이 \"HH:mm - HH:mm\" 형식으로 설정되어 있지 않습니다.");
		}
		LocalTime open;
		LocalTime close;
		try {
			open = LocalTime.parse(m.group(1), HH_MM);
			close = LocalTime.parse(m.group(2), HH_MM);
		} catch (DateTimeParseException e) {
			throw new IllegalArgumentException("매장 영업시간이 올바르지 않습니다: " + openingHours);
		}
		if (!open.isBefore(close)) {
			throw new IllegalArgumentException("자정을 넘기는 영업시간에는 시간대를 지정할 수 없습니다: " + openingHours);
		}
		return new LocalTime[] {open, close};
	}

	private static int minuteOfDay(LocalTime t) {
		return t.getHour() * 60 + t.getMinute();
	}
}
