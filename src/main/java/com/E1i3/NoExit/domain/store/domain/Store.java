package com.E1i3.NoExit.domain.store.domain;


import javax.persistence.*;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.dto.GameResDto;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import com.E1i3.NoExit.domain.store.dto.OwnerStoreSaveReqDto;
import com.E1i3.NoExit.domain.store.dto.StoreResDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Store {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 100, nullable = false)
	private String storeName;

	@Column(nullable = false)
	private int storeRating;

	private String openingHours;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private OpenStatus openStatus = OpenStatus.OPEN;

	private String phoneNumber;
	private String address;
	private String info;

	@Column(nullable = true)
	private Double latitude;  // 위도

	@Column(nullable = true)
	private Double longitude; // 경도
	@ManyToOne
	@JoinColumn(name = "owner_id")
	private Owner owner; //
	@OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Game> games = new ArrayList<>();

	// 사장님 매장 수정 (PUT 전체 교체). owner·storeRating 은 변경하지 않는다.
	public void update(OwnerStoreSaveReqDto dto) {
		this.storeName = dto.getStoreName();
		this.openingHours = dto.getOpeningHours();
		this.openStatus = dto.getOpenStatus() != null ? dto.getOpenStatus() : OpenStatus.OPEN;
		this.phoneNumber = dto.getPhoneNumber();
		this.address = dto.getAddress();
		this.info = dto.getInfo();
		this.latitude = dto.getLatitude();
		this.longitude = dto.getLongitude();
	}

	public void addGame(Game game) {
		this.games.add(game);
	}

	public StoreResDto fromEntity(){
		return StoreResDto.builder()
				.id(this.id)
				.storeName(this.storeName)
				.latitude(this.latitude)
				.longitude(this.longitude)
				.build();
	}
}