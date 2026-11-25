# 사장님 게임별 예약 시간대 (owner-timeslots)

브랜치: `l5/owner-timeslots` (main 병합·push 하지 않음)

## 현재
- `GameService.getAvailableHours` 가 `Store.openingHours`("HH:mm - HH:mm")를 1시간 간격으로 나눈다 (종료 시각 포함).
- `ReservationService.save` 는 `resDateTime` 을 검증하지 않는다.

## 결정
- 저장: **별도 엔티티 `GameTimeSlot`** (`game_id`, `start_time`, unique(game_id,start_time)).
  다른 worktree(검색 필터)와 충돌을 피하려고 `Game`/`Store` 엔티티는 **수정하지 않는다**.
- API: `PUT /owner/games/{gameId}/time-slots` body `{"startTimes":["10:00","11:30"]}` → 전체 교체, 200 + `{gameId, startTimes}` (정렬).
  `/owner/games/**` 는 이미 OWNER 전용. 본인 게임 아니면 400 (기존 관례), 없는 게임 404.
  빈 배열 = 사용자 지정 해제 → 1시간 단위 방식으로 복귀.
- 검증(`GameTimeSlotPolicy`, 순수 로직): 엄격한 `HH:mm`, 중복 없음, `open ≤ start`, `start + 러닝타임 ≤ close`.
  러닝타임은 자유 문자열이라 `"60"`, `"60분"`, `"1시간"`, `"1시간 30분"` 만 해석, 그 외는 400.
  영업시간 없음/형식 오류/자정 넘김이면 400.
- 조회: 시간대가 있으면 그것, 없으면 기존 1시간 단위 그대로.
- 예약 생성: Redis 락 전에 `resDateTime` 을 `LocalTime` 으로 파싱해 위 목록에 있는지 확인, 아니면 400.

## 범위 밖 (메모)
- 매장 영업시간을 나중에 줄이면 기존 시간대가 영업시간 밖에 남을 수 있음 (재검증 안 함).
- `"14:00"` / `"14:00:00"` 은 같은 시각으로 통과하지만 Redis 락 키 문자열은 다름 (기존부터 있던 문제).

## 작업
- [x] `GameTimeSlot` + `GameTimeSlotRepository` (bulk delete)
- [x] `GameTimeSlotPolicy` (검증, 러닝타임 해석, 1시간 단위 계산)
- [x] `OwnerGameService.replaceTimeSlots` + 컨트롤러 PUT
- [x] `GameService.getAvailableHours` 분기, `ReservationService.save` 검증
- [x] 단위 테스트, `./gradlew test`
- [x] bootRun(8081) 확인 후 PID 종료, 커밋
  - 확인: 정상 저장(정렬), 종료 초과/중복/형식/영업 전/null → 400, 타 사장님 400, USER 403,
    조회가 지정 시간대로 바뀜, 시간대 밖 예약 400·안 201, 빈 배열로 해제 시 1시간 단위 복귀, 같은 목록 연속 저장 OK.
