package com.E1i3.NoExit.domain.reservation.service;

import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.game.service.GameService;
import com.E1i3.NoExit.domain.member.repository.MemberRepository;
import com.E1i3.NoExit.domain.notification.controller.SseController;
import com.E1i3.NoExit.domain.notification.repository.NotificationRepository;
import com.E1i3.NoExit.domain.notification.service.NotificationService;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.reservation.domain.Reservation;
import com.E1i3.NoExit.domain.reservation.repository.ReservationRepository;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/* 취소된 예약(delYN=Y)은 예약된 시간대 계산에서 제외되어야 한다. */
@ExtendWith(MockitoExtension.class)
class ReservationAvailableHoursTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private GameRepository gameRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SseController sseController;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private GameService gameService;

    private ReservationService reservationService;
    private final LocalDate date = LocalDate.of(2026, 12, 1);

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, gameRepository, memberRepository,
                ownerRepository, storeRepository, sseController, notificationRepository, notificationService, gameService);
    }

    private Reservation reservation(String time) {
        return Reservation.builder().resDate(date).resDateTime(time).build();
    }

    @Test
    void slotIsBlockedBeforeCancel_andOpenAfterCancel() {
        Reservation r = reservation("10:00");
        when(reservationRepository.findByGameIdAndResDate(100L, date)).thenReturn(List.of(r));

        assertEquals(List.of("10:00"), reservationService.findAvailableHours(100L, date));

        r.updateDelYN();

        assertTrue(reservationService.findAvailableHours(100L, date).isEmpty());
    }

    @Test
    void onlyActiveReservationsBlockSlots() {
        Reservation canceled = reservation("10:00");
        canceled.updateDelYN();
        Reservation active = reservation("11:00");
        when(reservationRepository.findByGameIdAndResDate(100L, date)).thenReturn(List.of(canceled, active));

        assertEquals(List.of("11:00"), reservationService.findAvailableHours(100L, date));
    }
}
