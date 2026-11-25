package com.E1i3.NoExit.domain.game.dto;

public enum GameSortType {
    LATEST,     // createdTime desc
    PRICE_ASC,
    PRICE_DESC,
    DISTANCE    // 근처 검색(lat/lng/radiusKm) 필수
}
