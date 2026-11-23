package com.E1i3.NoExit.domain.store.dto;

import com.E1i3.NoExit.domain.store.domain.OpenStatus;
import com.E1i3.NoExit.domain.store.domain.Store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OwnerStoreResDto {
	private Long id;
	private String storeName;
	private int storeRating;
	private String openingHours;
	private OpenStatus openStatus;
	private String phoneNumber;
	private String address;
	private String info;
	private Double latitude;
	private Double longitude;
	private Long ownerId;

	public static OwnerStoreResDto from(Store store) {
		return OwnerStoreResDto.builder()
			.id(store.getId())
			.storeName(store.getStoreName())
			.storeRating(store.getStoreRating())
			.openingHours(store.getOpeningHours())
			.openStatus(store.getOpenStatus())
			.phoneNumber(store.getPhoneNumber())
			.address(store.getAddress())
			.info(store.getInfo())
			.latitude(store.getLatitude())
			.longitude(store.getLongitude())
			.ownerId(store.getOwner() != null ? store.getOwner().getId() : null)
			.build();
	}
}
