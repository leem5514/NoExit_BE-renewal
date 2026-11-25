package com.E1i3.NoExit.domain.game.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GameTimeSlotPolicyTest {

	private static final String HOURS = "10:00 - 22:00";

	private static List<LocalTime> validate(String... times) {
		return GameTimeSlotPolicy.validate(Arrays.asList(times), HOURS, "60분");
	}

	@Test
	void valid_times_are_returned_sorted() {
		assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(13, 30), LocalTime.of(21, 0)),
			validate("21:00", "10:00", "13:30"));
	}

	@Test
	void last_slot_may_end_exactly_at_closing() {
		assertEquals(List.of(LocalTime.of(21, 0)), validate("21:00"));
	}

	@Test
	void slot_ending_after_closing_is_rejected() {
		assertThrows(IllegalArgumentException.class, () -> validate("21:01"));
	}

	@Test
	void slot_before_opening_is_rejected() {
		assertThrows(IllegalArgumentException.class, () -> validate("09:59"));
	}

	@Test
	void slot_at_closing_time_is_rejected() {
		assertThrows(IllegalArgumentException.class, () -> validate("22:00"));
	}

	@Test
	void duplicate_times_are_rejected() {
		assertThrows(IllegalArgumentException.class, () -> validate("10:00", "11:00", "10:00"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"9:00", "10:0", "10:00:00", "24:00", "12:60", "1000", " 10:00", "abc", ""})
	void non_hh_mm_formats_are_rejected(String raw) {
		assertThrows(IllegalArgumentException.class, () -> validate(raw));
	}

	@Test
	void null_entry_is_rejected() {
		assertThrows(IllegalArgumentException.class, () -> validate("10:00", null));
	}

	@Test
	void null_list_is_rejected_and_empty_list_is_allowed() {
		assertThrows(IllegalArgumentException.class, () -> GameTimeSlotPolicy.validate(null, HOURS, "60분"));
		assertEquals(Collections.emptyList(), GameTimeSlotPolicy.validate(List.of(), HOURS, "60분"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"22:00 - 02:00", "10:00-22:00", "", "10:00 - 25:00"})
	void unusable_opening_hours_are_rejected(String hours) {
		assertThrows(IllegalArgumentException.class,
			() -> GameTimeSlotPolicy.validate(List.of("10:00"), hours, "60분"));
	}

	@Test
	void null_opening_hours_is_rejected() {
		assertThrows(IllegalArgumentException.class,
			() -> GameTimeSlotPolicy.validate(List.of("10:00"), null, "60분"));
	}

	@ParameterizedTest
	@CsvSource({"60,60", "60분,60", "75 분,75", "90min,90", "1시간,60", "1시간 30분,90", "2시간30분,150", " 45분 ,45"})
	void running_time_is_parsed_to_minutes(String raw, int expected) {
		assertEquals(expected, GameTimeSlotPolicy.runningMinutes(raw));
	}

	@ParameterizedTest
	@ValueSource(strings = {"한 시간", "60~70분", "0분", "", "1.5시간"})
	void unparseable_running_time_is_rejected(String raw) {
		assertThrows(IllegalArgumentException.class, () -> GameTimeSlotPolicy.runningMinutes(raw));
	}

	@Test
	void running_time_is_considered_against_closing() {
		// 90분 게임: 20:30 시작 → 22:00 종료 OK, 20:31 → 초과
		assertEquals(List.of(LocalTime.of(20, 30)),
			GameTimeSlotPolicy.validate(List.of("20:30"), HOURS, "1시간 30분"));
		assertThrows(IllegalArgumentException.class,
			() -> GameTimeSlotPolicy.validate(List.of("20:31"), HOURS, "1시간 30분"));
	}

	@Test
	void hourly_slots_keep_legacy_behavior_including_closing_hour() {
		assertEquals(List.of(LocalTime.of(10, 0), LocalTime.of(11, 0), LocalTime.of(12, 0)),
			GameTimeSlotPolicy.hourlySlots("10:00 - 12:00"));
	}
}
