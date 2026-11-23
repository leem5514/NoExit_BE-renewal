package com.E1i3.NoExit.domain.reservation.dto;

import com.E1i3.NoExit.domain.reservation.domain.ApprovalStatus;
import com.E1i3.NoExit.domain.reservation.domain.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/* 예약 생성 응답. 엔티티(Owner/Member 포함)를 직접 직렬화하지 않기 위한 DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateResDto {
    private Long id;
    private String resName;
    private String phoneNumber;
    private int numberOfPlayers;
    private LocalDate resDate;
    private String resDateTime;
    private Long gameId;
    private String gameName;
    private String storeName;
    private ReservationStatus reservationStatus;
    private ApprovalStatus approvalStatus;
}
