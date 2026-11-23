package com.E1i3.NoExit.domain.reservation.service;

import com.E1i3.NoExit.domain.common.domain.DelYN;
import com.E1i3.NoExit.domain.game.domain.Game;
import com.E1i3.NoExit.domain.game.repository.GameRepository;
import com.E1i3.NoExit.domain.member.domain.Member;
import com.E1i3.NoExit.domain.member.repository.MemberRepository;
import com.E1i3.NoExit.domain.notification.controller.SseController;
import com.E1i3.NoExit.domain.notification.domain.NotificationType;
import com.E1i3.NoExit.domain.notification.dto.NotificationResDto;
import com.E1i3.NoExit.domain.notification.repository.NotificationRepository;
import com.E1i3.NoExit.domain.notification.service.NotificationService;
import com.E1i3.NoExit.domain.owner.domain.Owner;
import com.E1i3.NoExit.domain.owner.repository.OwnerRepository;
import com.E1i3.NoExit.domain.reservation.domain.ApprovalStatus;
import com.E1i3.NoExit.domain.reservation.domain.Reservation;
import com.E1i3.NoExit.domain.reservation.domain.ReservationStatus;
import com.E1i3.NoExit.domain.reservation.repository.ReservationRepository;
import com.E1i3.NoExit.domain.store.domain.Store;
import com.E1i3.NoExit.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationOwnerCancelTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private GameRepository gameRepository;
    @Mock private OwnerRepository ownerRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private SseController sseController;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationService notificationService;
    @Mock private RedisTemplate<String, Object> reservationRedisTemplate;

    private ReservationService reservationService;

    private final Owner myOwner = Owner.builder().id(1L).email("owner@test.com").storeName("My Store").build();
    private final Owner otherOwner = Owner.builder().id(2L).email("other@test.com").storeName("Other Store").build();
    private final Member member = Member.builder().email("user@test.com").nickname("user").build();

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(reservationRepository, gameRepository, memberRepository,
                ownerRepository, storeRepository, sseController, notificationRepository, notificationService);
        ReflectionTestUtils.setField(reservationService, "reservationRedisTemplate", reservationRedisTemplate);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("owner@test.com", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Reservation reservationOf(Owner storeOwner) {
        Store store = Store.builder().id(10L).storeName(storeOwner.getStoreName()).owner(storeOwner).build();
        Game game = Game.builder().id(100L).gameName("Escape").store(store).build();
        Reservation reservation = Reservation.builder()
                .owner(storeOwner).member(member).game(game)
                .resDate(LocalDate.of(2026, 12, 1)).resDateTime("14:00")
                .reservationStatus(ReservationStatus.WAITING).approvalStatus(ApprovalStatus.READY)
                .build();
        reservation.setId(5L);
        return reservation;
    }

    @Test
    void cancelByOwner_success_cancels_and_notifies_member_with_reason() {
        Reservation reservation = reservationOf(myOwner);
        when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        Reservation result = reservationService.cancelByOwner(5L, "  설비 점검  ");

        assertEquals(ReservationStatus.CANCELED, result.getReservationStatus());
        assertEquals("설비 점검", result.getCancelReason());
        verify(reservationRedisTemplate).delete("reservation:lock:My Store:Escape:2026-12-01:14:00");

        ArgumentCaptor<NotificationResDto> captor = ArgumentCaptor.forClass(NotificationResDto.class);
        verify(sseController).publishMessage(captor.capture(), eq("user@test.com"));
        NotificationResDto sent = captor.getValue();
        assertEquals("user@test.com", sent.getEmail());
        assertEquals("owner@test.com", sent.getSender_email());
        assertEquals(NotificationType.RESERVATION_CANCEL, sent.getType());
        assertEquals(5L, sent.getNotification_id());
        assertTrue(sent.getMessage().contains("설비 점검"));
    }

    @Test
    void cancelByOwner_rejects_reservation_of_other_store() {
        Reservation reservation = reservationOf(otherOwner);
        when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalArgumentException.class, () -> reservationService.cancelByOwner(5L, "사유"));

        assertEquals(ReservationStatus.WAITING, reservation.getReservationStatus());
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(sseController, reservationRedisTemplate);
    }

    @Test
    void cancelByOwner_rejects_already_canceled_reservation() {
        Reservation reservation = reservationOf(myOwner);
        reservation.cancelByOwner("첫 취소");
        when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelByOwner(5L, "두 번째"));

        assertEquals("첫 취소", reservation.getCancelReason());
        verify(reservationRepository, never()).save(any());
        verifyNoInteractions(sseController);
    }

    @Test
    void cancelByOwner_rejects_reservation_already_canceled_by_member() {
        Reservation reservation = reservationOf(myOwner);
        reservation.setDelYN(DelYN.Y);
        when(ownerRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(myOwner));
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThrows(IllegalStateException.class, () -> reservationService.cancelByOwner(5L, "사유"));
        verifyNoInteractions(sseController);
    }

    @Test
    void cancelByOwner_requires_reason() {
        assertThrows(IllegalArgumentException.class, () -> reservationService.cancelByOwner(5L, null));
        assertThrows(IllegalArgumentException.class, () -> reservationService.cancelByOwner(5L, "   "));
        verifyNoInteractions(reservationRepository, sseController);
    }
}
