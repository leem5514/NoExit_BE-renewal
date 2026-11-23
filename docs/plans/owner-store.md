# 사장님 매장·게임 등록/수정 (owner-store)

## 배경 (현재 코드 상태)
- `Store`/`Game` 을 생성·수정하는 API가 **전혀 없고**, `InitialDataLoader` 도 매장·게임을 시드하지 않는다.
  → 지금은 SQL 없이 예약 흐름을 시작할 수 없다. 이번 기능이 그 출발점이 된다.
- `Store` (`store/domain/Store.java`): `@ManyToOne Owner owner` (FK `owner_id`, Store 쪽이 소유), `@OneToMany(mappedBy="store", cascade=ALL, orphanRemoval=true) List<Game> games` (`@Builder.Default` 없음 → 빌더로 만들면 null). 수정 메서드 없음. BaseTimeEntity 미상속.
- `Game` (`game/domain/Game.java`): `@ManyToOne(LAZY) Store store` (`store_id`, NOT NULL). `runningTime` 은 자유 문자열, `maximunPerson`(오타 그대로 유지), `Difficult` ONE~FIVE, `AgeLimit` ADULT/STUDENT, `imagePath` nullable. 수정 메서드 없음.
- `Owner.storeName` 은 `Store.storeName` 과 별개 필드이며 Owner → Store 역참조가 없다.
- 영업시간: `Store.openingHours` 단일 문자열, 형식 **`"HH:mm - HH:mm"`** (공백-하이픈-공백). `GameService.getAvailableHours` / `Game.getAvailableHours` 가 `split(" - ")` + `LocalTime.parse` 후 1시간 간격으로 슬롯을 만든다. null·형식 오류·자정 넘김(`22:00 - 02:00`)은 예외 또는 잘못된 결과.
- 권한 패턴(기존 사장님 예약 취소 `ReservationService.cancelByOwner`): SecurityContext 이메일 → `ownerRepository.findByEmail` → 대상의 owner id 비교 → 불일치 시 `IllegalArgumentException("본인 매장의 … 만 …")` (400). 응답은 `CommonResDto`, 예외는 `CommonExceptionHandler`.
- `SecurityConfigs` 에서 `/game/**` 가 **permitAll** 이다. 게임 쓰기 API를 `/game/**` 아래 두면 URL 레벨 보호가 없다.

## 결정 사항

### API 경로
기존 `/owner/**` 네임스페이스 아래에 두고(`/game/**` permitAll 회피), 응답은 모두 `CommonResDto`.

| 메서드 | 경로 | 설명 | 성공 응답 |
|---|---|---|---|
| POST | `/owner/stores` | 내 매장 등록 | 201, `result` = 매장 상세(id 포함) |
| PUT | `/owner/stores/{storeId}` | 내 매장 수정 | 200, 매장 상세 |
| GET | `/owner/stores` | 내 매장 목록 (검증 단계에서 id 확인용) | 200, 목록 |
| POST | `/owner/stores/{storeId}/games` | 내 매장에 게임 등록 | 201, 게임 상세(id 포함) |
| PUT | `/owner/games/{gameId}` | 내 게임 수정 | 200, 게임 상세 |

- 수정은 **PUT 전체 교체**(필수 필드 모두 전송). 부분 수정(PATCH)은 이번 범위 아님.
- 삭제 API는 요구사항에 없으므로 제외.
- 새 컨트롤러: `OwnerStoreController` (store 도메인), 서비스: `OwnerStoreService` / `OwnerGameService` (또는 기존 `StoreService`/`GameService`에 메서드 추가 — 기존 서비스가 조회 위주라 **별도 서비스로 분리**하기로 함).
- 응답 DTO: `OwnerStoreResDto`, `OwnerGameResDto` (기존 `StoreResDto` 는 id·이름·좌표만 있어 재사용하지 않음). 엔티티를 직접 반환하지 않는다.

### 권한 규칙
- URL 레벨: `SecurityConfigs` 에 `.antMatchers("/owner/stores/**", "/owner/games/**").hasRole("OWNER")` 추가 (`/owner/stores` 자체도 포함되도록 `"/owner/stores"` 도 명시). USER 토큰은 필터에서 403, 토큰 없음은 기존대로 거부.
- 메서드 레벨: 컨트롤러에 `@PreAuthorize("hasRole('OWNER')")` (기존 관례와 동일).
- 소유권: 현재 이메일 → `ownerRepository.findByEmail` (없으면 `IllegalArgumentException("등록되지 않은 점장 이메일입니다.")`).
  - 매장 수정 / 게임 등록: `store.getOwner().getId()` 와 현재 owner id 비교.
  - 게임 수정: `game.getStore().getOwner().getId()` 와 비교.
  - 불일치 → `IllegalArgumentException("본인 매장만 수정할 수 있습니다.")` / `("본인 매장의 게임만 …")` → **400** (기존 cancelByOwner 와 동일한 관례 유지. 403 전환은 전역 정책 변경이라 이번엔 하지 않음).
  - 대상 없음 → `EntityNotFoundException` → 404.
- 매장 등록 시 owner 는 **요청 바디가 아니라 토큰에서만** 결정. 수정 시 owner·store 소속 변경 불가.
- 한 사장님이 여러 매장을 가질 수 있음 (기존 ManyToOne 구조 그대로).

### 검증 규칙 (`@Valid` + Bean Validation, 위반 시 기존 핸들러로 400)
매장 (`OwnerStoreSaveReqDto`, 등록·수정 공용):
- `storeName`: `@NotBlank`, `@Size(max=100)` (컬럼 길이)
- `openingHours`: `@NotBlank`, 정규식 `^([01]\d|2[0-3]):[0-5]\d - ([01]\d|2[0-3]):[0-5]\d$`, 그리고 **시작 < 종료** (자정 넘김 불가 — 기존 슬롯 계산이 지원하지 않음). 커스텀 제약 `@OpeningHours` 또는 서비스에서 `IllegalArgumentException`.
- `openStatus`: 선택, 미지정 시 `OPEN`.
- `phoneNumber`, `address`, `info`: 선택 (`address` `@Size(max=255)`).
- `latitude` −90~90, `longitude` −180~180, 선택.
- `storeRating`: 요청에서 받지 않음. 등록 시 0, 수정 시 유지.

게임 (`OwnerGameSaveReqDto`, 등록·수정 공용):
- `gameName`: `@NotBlank`, `@Size(max=255)`
- `runningTime`: `@NotBlank` (기존처럼 자유 문자열, 예: `"60분"`)
- `difficult`: `@NotNull` (ONE~FIVE)
- `price`: `@NotNull @Min(0)`
- `maximunPerson`: `@NotNull @Min(1)` (JSON 필드명도 기존 엔티티 철자 유지)
- `ageLimit`: `@NotNull`
- `gameInfo`: 선택
- `wishCount`: 요청에서 받지 않음. 등록 시 0, 수정 시 유지.
- `imagePath`: **이번 범위 제외** (S3 로컬 미동작). 등록 시 null, 수정 시 기존 값 유지. 추후 multipart `file` 선택 파트로 확장.

### 기존 엔티티와의 관계 / 엔티티 변경
- `Store`: `games` 에 `@Builder.Default = new ArrayList<>()` 추가, `update(OwnerStoreSaveReqDto)` 도메인 메서드 추가. owner·rating 은 건드리지 않음.
- `Game`: `update(OwnerGameSaveReqDto)` 도메인 메서드 추가. store·wishCount·imagePath 유지.
- 게임 등록은 `Game.builder().store(store)…` 후 `gameRepository.save` (Store.games 컬렉션에도 add 하여 양방향 일관성 유지).
- `Owner.storeName` 은 동기화하지 않음 (별개 필드, 회원가입 시 입력값).
- 테이블 구조 변경 없음 (`ddl-auto: create`, 컬럼 추가 없음).

### 알려진 위험 / 범위 밖
- `Reservation.game` 이 `@OneToOne` 이라 FK에 unique 제약이 생길 수 있음 → 게임당 예약 1건만 가능할 수 있다. 최종 검증은 예약 1건이라 영향 없음. 수정은 이번 범위 아님 (발견 시 보고).
- 예약 Redis 락 키에 매장명·게임명이 들어가므로 이름 수정 후에는 키가 달라진다. 기능상 문제는 없음을 메모만.
- `@PreAuthorize` 위반 시 `AccessDeniedException` 이 500 으로 나갈 수 있음 → URL 레벨 `hasRole` 로 막으므로 이번 API에선 403. 전역 핸들러 수정은 범위 밖.
- 예약 생성 시 영업시간·최대 인원 검증이 없음 (기존 동작). 범위 밖.

## 체크리스트

### 구현
- [x] `Store`: `@Builder.Default games`, `update(...)` 추가
- [x] `Game`: `update(...)` 추가
- [x] DTO: `OwnerStoreSaveReqDto`, `OwnerStoreResDto`, `OwnerGameSaveReqDto`, `OwnerGameResDto` (+ 영업시간 검증)
- [x] `OwnerStoreService`: `create`, `update`, `myStores` (소유권 검사 포함)
- [x] `OwnerGameService`: `create(storeId, dto)`, `update(gameId, dto)` (소유권 검사 포함)
- [x] `OwnerStoreController`: 5개 엔드포인트, `@PreAuthorize("hasRole('OWNER')")`, `@Valid`
- [x] `SecurityConfigs`: `/owner/stores`, `/owner/stores/**`, `/owner/games/**` → `hasRole("OWNER")`

### 단위 테스트 (JUnit5 + Mockito, `ReservationOwnerCancelTest` 스타일, 패키지 경로에 맞게 배치)
- [x] 매장 등록: 토큰 이메일의 owner 로 저장, rating 0 / openStatus 기본 OPEN
- [x] 매장 등록: 미등록 이메일 → `IllegalArgumentException`, save 미호출
- [x] 매장 수정: 본인 매장 → 필드 반영, owner·rating 유지
- [x] 매장 수정: 타인 매장 → 예외, 변경 없음 / 없는 매장 → `EntityNotFoundException`
- [x] 영업시간 검증: `"10:00 - 22:00"` 통과, `"10:00-22:00"`, `"25:00 - 22:00"`, `"22:00 - 10:00"`, `"22:00 - 02:00"`, 빈값 거부
- [x] 게임 등록: 본인 매장 → store 연결·wishCount 0·imagePath null 로 저장
- [x] 게임 등록: 타인 매장 → 예외, save 미호출
- [x] 게임 수정: 본인 게임 → 반영, store·wishCount·imagePath 유지 / 타인 게임 → 예외
- [x] DTO 검증: 필수 누락·`price < 0`·`maximunPerson < 1` 위반 검출 (`Validation.buildDefaultValidatorFactory()`)
- [x] `./gradlew test` 통과 (기존 `NoExitApplicationTests` 는 DB/Redis 필요 — 로컬 환경 확인 후 실행)

### 최종 확인 (SQL 없이 API만, 한글 바디는 `printf` → `curl --data-binary @file`)
- [x] `./gradlew bootRun` 기동 (8080)
- [x] `POST /doLogin` `owner@test.com` / OWNER → 사장님 토큰
- [x] `POST /owner/stores` (`openingHours: "10:00 - 22:00"`) → storeId
- [x] `PUT /owner/stores/{storeId}` 수정 반영 확인
- [x] `POST /owner/stores/{storeId}/games` → gameId, `PUT /owner/games/{gameId}` 수정 반영 확인
- [x] 음성 케이스: USER 토큰으로 `POST /owner/stores` → 403, 잘못된 영업시간 → 400
- [x] `GET /game/{gameId}/available-hours` 로 슬롯 생성 확인
- [x] `POST /doLogin` `user@test.com` / USER → `POST /reservation/create` (gameId, 미래 날짜, 슬롯 시간)
- [x] 사장님 `GET /reservation/storeReservation` 에서 예약 확인 → `PUT /reservation/owner/cancel/{id}` (`{"reason": "..."}`) → CANCELED 확인
- [x] 앱 종료: `netstat -ano | grep LISTENING | grep ':8080 '` → `taskkill //PID <pid> //F`
- [x] 커밋 (push 하지 않음)
