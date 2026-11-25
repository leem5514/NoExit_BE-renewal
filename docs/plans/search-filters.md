# 게임 복합 조건 검색 (search-filters)

## 현재 상태
- `GameSearchDto`(gameName, price:String, difficult:String)는 **어디서도 쓰이지 않는다** → 타입을 바꿔 확장해도 영향 없음.
- `Store.latitude/longitude` 는 nullable Double, 공간 인덱스 없음. DB는 MariaDB, `ddl-auto: create`.
- 기존 동적 검색 관례: `FindBoardService` 의 JPA `Specification` + `JpaSpecificationExecutor`.
- `/game/**` 는 permitAll → 검색 API는 비로그인도 사용 가능.

## API
`GET /game/search` (응답 `CommonResDto`, `result` = `Page<GameSearchResDto>`)

| 파라미터 | 의미 |
|---|---|
| `gameName` | 게임명 부분 일치 (기존 필드 유지) |
| `region` | 매장 주소 부분 일치 (`store.address LIKE %region%`) |
| `difficult` | 여러 개: `difficult=TWO&difficult=THREE` (IN) |
| `minPrice`, `maxPrice` | 가격 범위(포함) |
| `person` | `maximunPerson >= person` |
| `lat`, `lng`, `radiusKm` | 근처 검색. 셋 다 주거나 셋 다 생략. `0 < radiusKm <= 50` |
| `sort` | `LATEST`(기본, createdTime desc) / `PRICE_ASC` / `PRICE_DESC` / `DISTANCE`(근처 검색 필수) |
| `page`, `size` | 기본 0 / 16, size 최대 100 |

- 검증 실패(범위 역전, 좌표 일부만, DISTANCE인데 좌표 없음 등) → `IllegalArgumentException` → 400.
- 잘못된 enum 값 등 바인딩 오류(`BindException`) → 지금은 500으로 떨어지므로 `CommonExceptionHandler` 에 400 핸들러 추가.
- 응답 DTO는 새로 `GameSearchResDto` (기존 `GameResDto` 필드 + maximunPerson, storeAddress, distanceKm). 기존 목록 API 응답은 건드리지 않는다.

## 거리 계산: 바운딩 박스(DB) + 메모리 정밀 계산 — 선택
- DB: 나머지 필터와 함께 `latitude BETWEEN ? AND ?`, `longitude BETWEEN ? AND ?` 로 후보를 좁힌다 (반경을 감싸는 사각형).
- 메모리: 후보마다 Haversine 거리 계산 → `radiusKm` 밖 제외 → 정렬 → 페이지 자르기.
- 이유
  1. JPQL에는 삼각함수가 없어 DB Haversine은 네이티브 쿼리/방언 함수 등록이 필요하고, 나머지 선택 필터와 조합하려면 동적 네이티브 SQL을 직접 짜야 한다. 바운딩 박스는 단순 비교라 `Specification` 에 그대로 합쳐진다.
  2. 거리 정렬은 계산된 값 기준이라 Spring `Pageable` sort로 표현이 안 된다. 반경을 최대 50km로 제한하면 후보 수가 매장·게임 규모(수천 이하)에서 작아 메모리 정렬/페이징 비용이 무시할 만하다.
  3. 거리 계산·반경 판정이 순수 Java라 DB 없이 단위 테스트가 가능하다.
- 트레이드오프: 후보가 매우 많아지면(전국 규모 반경) 메모리 비용이 커진다 → 반경 상한으로 차단. 나중에 규모가 커지면 `(latitude, longitude)` 인덱스 또는 MariaDB 공간 타입으로 옮긴다. (지금은 다른 worktree가 Store를 수정 중이라 인덱스 추가는 보류.)
- 근처 검색이 **없으면** 기존처럼 DB에서 정렬·페이징 (`findAll(spec, pageable)`).

## 엔티티 변경
- 없음. `GameRepository` 에 `JpaSpecificationExecutor<Game>` 만 추가.

## 작업
- [x] `GameSearchDto` 확장 + 검증 메서드, `GameSortType` enum
- [x] `GameSpecifications` (필터 조합 + 바운딩 박스), `GeoUtils` (Haversine, 바운딩 박스)
- [x] `GameService.search`, `GameController` `GET /game/search`
- [x] `CommonExceptionHandler` BindException → 400
- [x] 단위 테스트: GeoUtils, 검증, 서비스(근처 필터·거리 정렬·페이징, DB 경로 위임)
- [x] bootRun(8082) + 사장님 API로 매장·게임 만들어 curl 확인

## 메모
- LIKE ESCAPE 문자는 `!` (MariaDB 리터럴에서 `\` 가 이스케이프로 해석됨).
- curl 로 한글 쿼리를 보낼 때 인자를 그대로 넣으면 CP949로 인코딩돼 결과가 0건 → UTF-8 퍼센트 인코딩(`region=%EA%B0%95%EB%82%A8`)으로 확인.
