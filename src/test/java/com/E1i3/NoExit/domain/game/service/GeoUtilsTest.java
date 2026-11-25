package com.E1i3.NoExit.domain.game.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GeoUtilsTest {

	// 강남역, 서울역, 부산역 (근사 좌표)
	private static final double GANGNAM_LAT = 37.4979, GANGNAM_LNG = 127.0276;
	private static final double SEOUL_LAT = 37.5547, SEOUL_LNG = 126.9707;
	private static final double BUSAN_LAT = 35.1151, BUSAN_LNG = 129.0422;

	@Test
	void distance_samePoint_isZero() {
		assertEquals(0.0, GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, GANGNAM_LAT, GANGNAM_LNG), 1e-9);
	}

	@Test
	void distance_knownPairs() {
		assertEquals(8.0, GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, SEOUL_LAT, SEOUL_LNG), 0.5);
		assertEquals(325.0, GeoUtils.distanceKm(SEOUL_LAT, SEOUL_LNG, BUSAN_LAT, BUSAN_LNG), 5.0);
	}

	@Test
	void distance_isSymmetric() {
		assertEquals(
			GeoUtils.distanceKm(SEOUL_LAT, SEOUL_LNG, BUSAN_LAT, BUSAN_LNG),
			GeoUtils.distanceKm(BUSAN_LAT, BUSAN_LNG, SEOUL_LAT, SEOUL_LNG), 1e-9);
	}

	@Test
	void boundingBox_containsCircle() {
		double r = 10.0;
		GeoUtils.BoundingBox box = GeoUtils.boundingBox(GANGNAM_LAT, GANGNAM_LNG, r);
		// 사각형의 각 변 중점까지의 거리는 반경과 같다 (원이 사각형에 내접)
		assertEquals(r, GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, box.maxLat, GANGNAM_LNG), 0.01);
		assertEquals(r, GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, box.minLat, GANGNAM_LNG), 0.01);
		assertTrue(GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, GANGNAM_LAT, box.maxLng) >= r - 0.01);
		assertTrue(GeoUtils.distanceKm(GANGNAM_LAT, GANGNAM_LNG, GANGNAM_LAT, box.minLng) >= r - 0.01);
	}

	@Test
	void boundingBox_clampsNearPole() {
		GeoUtils.BoundingBox box = GeoUtils.boundingBox(89.99, 0, 50);
		assertEquals(90.0, box.maxLat);
		assertTrue(box.minLng >= -180.0 && box.maxLng <= 180.0);
	}
}
