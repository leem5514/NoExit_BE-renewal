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
