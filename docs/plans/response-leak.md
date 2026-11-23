# 예약 응답 비밀번호 노출 / 시드 사장님 비밀번호 (response-leak)

브랜치: `fix/reservation-response-leak` (main 병합·push 하지 않음)

## 문제
1. `POST /reservation/create` 가 `Reservation` 엔티티를 그대로 `CommonResDto.result` 에 담는다.
   `Reservation.owner`(`@ManyToOne Owner`)에 `@JsonIgnore` 가 없어 `owner.password`(bcrypt 해시)·email·phoneNumber 가 예약한 사용자에게 나간다.
2. `InitialDataLoader` 의 `test@test.com` 사장님 비밀번호가 평문 `"12341234"` 로 저장돼 로그인 불가.

## 엔티티 직접 응답 전수 조사 (컨트롤러 기준)
| API | 반환 | 비밀번호 노출 | 조치 |
|---|---|---|---|
| `POST /reservation/create` | `Reservation` (→ Owner) | **있음** (Owner 해시) | 응답 DTO로 교체 |
| `POST /chat/createRoom`, `GET /chat/rooms`, `GET /chat/rooms/{id}`, `GET /chat/myrooms` | `ChatRoom` | **있음** (채팅방 비밀번호 평문) | `ChatRoom.password` 에 `@JsonIgnore` |
| `GET /chat/rooms/{roomId}/messages` (ChatController) | `List<ChatMessageEntity>` | 없음 (`chatRoom` 은 `@JsonBackReference`) | 보고만 |

나머지 컨트롤러는 DTO·id·Map 을 반환 (조사 완료).

## 작업
- [x] `ReservationCreateResDto` 추가 (id, resName, phoneNumber, numberOfPlayers, resDate, resDateTime, gameId, gameName, storeName, reservationStatus, approvalStatus) + `Reservation.toCreateResDto()`
- [x] `ReservationController.reservationCreate` 가 DTO 반환
- [x] `ChatRoom.password` `@JsonIgnore` (요청은 `CreateRoomRequest` 로 받으므로 영향 없음)
- [x] `InitialDataLoader` test@test.com → `passwordEncoder.encode("12341234")`
- [x] 테스트: 컨트롤러 응답을 ObjectMapper 로 직렬화해 `password` 문자열/해시가 없음을 검증, ChatRoom 직렬화에도 password 없음
- [x] `./gradlew test`
- [x] bootRun: test@test.com OWNER 로그인 성공, 예약 생성 응답에 password 없음, `/chat/rooms` 에 password 없음
- [x] 앱 PID 종료, CLAUDE.md 의 test@test.com 주의 문구 갱신, 커밋

## 리뷰 반영 (Codex)
| # | 지적 | 판정 | 조치 / 근거 |
|---|---|---|---|
| 1 | `MemberDetResDto.password` 로 해시 노출 (`/member/myInfo`) | 수용 | DTO 필드와 `Member.detFromEntity()` 매핑 제거 |
| 2 | `AuthChannelInterceptor` 가 STOMP 토큰 원문 출력 | 수용 | 헤더 존재 여부만 출력 |
| 3 | 기존 DB 의 평문 시드 행이 남음 | 부분 수용 | 보정 로직은 불필요: `application.yml` `ddl-auto: create` 고정, 덮어쓰는 env/k8s/compose 설정 없음, 기동 로그에 `drop table if exists owner` 확인. 시드 인코딩 테스트(신규·기존 존재)는 추가 |
| 4 | 채팅 테스트가 엔티티만 직렬화 | 수용 | 컨트롤러 4개 경로 + 생성 요청 비밀번호의 서비스 전달 검증 |
| 5 | 예약 응답 평탄화로 프론트 깨짐 가능 | 확인 불가 | 프론트 저장소 없음 |
| + | `POST /member/delete` 가 `Member` 엔티티 응답 (리뷰 외 추가 발견) | 수정 | 점주 탈퇴와 같게 id 반환 |

- [x] 새 테스트가 수정 전(main) 코드에서 실패함을 확인 (10개 중 9개 실패, 남은 1개는 동작 불변 검증)
- [x] `./gradlew test` 통과
- [x] bootRun: `/member/myInfo`·`/member/delete`·채팅방 4경로에 password 없음, SockJS STOMP CONNECT 성공 후 로그에 토큰 원문 0건
