package com.E1i3.NoExit.domain.game.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.E1i3.NoExit.domain.game.domain.GameTimeSlot;

public interface GameTimeSlotRepository extends JpaRepository<GameTimeSlot, Long> {

	List<GameTimeSlot> findByGameIdOrderByStartTimeAsc(Long gameId);

	// 전체 교체 시 즉시 DELETE 를 실행해야 같은 시각 재등록이 unique 제약에 걸리지 않는다
	// (Hibernate 는 flush 때 INSERT 를 DELETE 보다 먼저 실행한다).
	@Modifying(flushAutomatically = true, clearAutomatically = false)
	@Query("delete from GameTimeSlot s where s.game.id = :gameId")
	void deleteAllByGameId(@Param("gameId") Long gameId);
}
