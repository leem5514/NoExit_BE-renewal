package com.E1i3.NoExit.domain.store.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.E1i3.NoExit.domain.game.domain.AgeLimit;
import com.E1i3.NoExit.domain.game.domain.Difficult;
import com.E1i3.NoExit.domain.game.dto.OwnerGameSaveReqDto;

// 매장·게임 요청 DTO 의 Bean Validation 규칙 (컨트롤러 @Valid 에서 400 으로 나가는 규칙)
class OwnerSaveReqDtoValidationTest {

	private static ValidatorFactory factory;
	private static Validator validator;

	@BeforeAll
	static void init() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void close() {
		factory.close();
	}

	private OwnerStoreSaveReqDto store(String hours) {
		return OwnerStoreSaveReqDto.builder().storeName("Store").openingHours(hours).build();
	}

	private Set<String> invalidFields(Object dto) {
		return validator.validate(dto).stream()
			.map(ConstraintViolation::getPropertyPath)
			.map(Object::toString)
			.collect(Collectors.toSet());
	}

	@ParameterizedTest
	@ValueSource(strings = {"10:00 - 22:00", "00:00 - 23:59", "09:30 - 10:00"})
	void opening_hours_valid(String hours) {
		assertTrue(validator.validate(store(hours)).isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"10:00-22:00", "25:00 - 22:00", "10:60 - 22:00", "1000 - 2200", "10:00 ~ 22:00"})
	void opening_hours_bad_format_rejected(String hours) {
		assertEquals(Set.of("openingHours"), invalidFields(store(hours)));
	}

	@ParameterizedTest
	@ValueSource(strings = {"22:00 - 10:00", "22:00 - 02:00", "10:00 - 10:00"})
	void opening_hours_start_must_be_before_end(String hours) {
		assertEquals(Set.of("openingHoursOrdered"), invalidFields(store(hours)));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"   "})
	void opening_hours_required(String hours) {
		assertTrue(invalidFields(store(hours)).contains("openingHours"));
	}

	@Test
	void store_name_required_and_coordinates_bounded() {
		OwnerStoreSaveReqDto dto = OwnerStoreSaveReqDto.builder()
			.storeName(" ").openingHours("10:00 - 22:00").latitude(91.0).longitude(-181.0).build();
		assertEquals(Set.of("storeName", "latitude", "longitude"), invalidFields(dto));
	}

	@Test
	void game_valid_request_passes() {
		OwnerGameSaveReqDto dto = OwnerGameSaveReqDto.builder()
			.gameName("Escape").runningTime("60분").difficult(Difficult.TWO)
			.price(0).maximunPerson(1).ageLimit(AgeLimit.ADULT).build();
		assertTrue(validator.validate(dto).isEmpty());
	}

	@Test
	void game_missing_required_fields_rejected() {
		assertEquals(Set.of("gameName", "runningTime", "difficult", "price", "maximunPerson", "ageLimit"),
			invalidFields(new OwnerGameSaveReqDto()));
	}

	@Test
	void game_negative_price_and_zero_person_rejected() {
		OwnerGameSaveReqDto dto = OwnerGameSaveReqDto.builder()
			.gameName("Escape").runningTime("60분").difficult(Difficult.TWO)
			.price(-1).maximunPerson(0).ageLimit(AgeLimit.ADULT).build();
		assertEquals(Set.of("price", "maximunPerson"), invalidFields(dto));
	}
}
