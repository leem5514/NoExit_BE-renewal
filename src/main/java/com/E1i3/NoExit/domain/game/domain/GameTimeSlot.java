package com.E1i3.NoExit.domain.game.domain;

import java.time.LocalTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 사장님이 지정한 게임별 예약 시작 시각. Game 엔티티를 건드리지 않으려고 별도 테이블로 둔다.
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game_time_slot",
	uniqueConstraints = @UniqueConstraint(name = "uk_game_time_slot", columnNames = {"game_id", "start_time"}))
public class GameTimeSlot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "game_id", nullable = false)
	private Game game;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	public GameTimeSlot(Game game, LocalTime startTime) {
		this.game = game;
		this.startTime = startTime;
	}
}
