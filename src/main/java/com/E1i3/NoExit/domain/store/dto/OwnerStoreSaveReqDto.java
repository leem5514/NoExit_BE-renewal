package com.E1i3.NoExit.domain.store.dto;

import java.time.LocalTime;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.store.domain.OpenStatus;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사장님 매장 등록·수정 공용 요청 DTO. owner·storeRating 은 받지 않는다.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerStoreSaveReqDto {

	// 기존 슬롯 계산(Game.getAvailableHours)이 split(" - ") 하는 형식: "HH:mm - HH:mm"
	public static final String OPENING_HOURS_REGEX = "^([01]\\d|2[0-3]):[0-5]\\d - ([01]\\d|2[0-3]):[0-5]\\d$";

	@NotBlank(message = "매장명은 필수입니다.")
	@Size(max = 100, message = "매장명은 100자 이하여야 합니다.")
	private String storeName;

	@NotBlank(message = "영업시간은 필수입니다.")
	@Pattern(regexp = OPENING_HOURS_REGEX, message = "영업시간은 \"HH:mm - HH:mm\" 형식이어야 합니다.")
	private String openingHours;

	private OpenStatus openStatus; // 미지정 시 OPEN

	private String phoneNumber;

	@Size(max = 255, message = "주소는 255자 이하여야 합니다.")
	private String address;

	private String info;

	@DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 이어야 합니다.")
	@DecimalMax(value = "90.0", message = "위도는 -90 ~ 90 이어야 합니다.")
	private Double latitude;

	@DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 이어야 합니다.")
	@DecimalMax(value = "180.0", message = "경도는 -180 ~ 180 이어야 합니다.")
	private Double longitude;

	// 시작 < 종료 (자정을 넘기는 영업시간은 기존 슬롯 계산이 지원하지 않음).
	// 형식 오류는 @Pattern 이 잡으므로 여기서는 통과시킨다.
	@JsonIgnore
	@AssertTrue(message = "영업 시작 시간은 종료 시간보다 빨라야 합니다.")
	public boolean isOpeningHoursOrdered() {
		if (openingHours == null || !openingHours.matches(OPENING_HOURS_REGEX)) {
			return true;
		}
		String[] times = openingHours.split(" - ");
		return LocalTime.parse(times[0]).isBefore(LocalTime.parse(times[1]));
	}

	public Store toEntity(Owner owner) {
		return Store.builder()
			.storeName(storeName)
			.storeRating(0)
			.openingHours(openingHours)
			.openStatus(openStatus != null ? openStatus : OpenStatus.OPEN)
			.phoneNumber(phoneNumber)
			.address(address)
			.info(info)
			.latitude(latitude)
			.longitude(longitude)
			.owner(owner)
			.build();
	}
}
