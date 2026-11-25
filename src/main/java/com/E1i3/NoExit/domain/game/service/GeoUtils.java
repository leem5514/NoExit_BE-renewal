package com.E1i3.NoExit.domain.game.service;

// 거리 계산 유틸. 지구를 반지름 6371km 구로 근사한다.
public final class GeoUtils {

	public static final double EARTH_RADIUS_KM = 6371.0;

	private GeoUtils() {
	}

	// Haversine 거리(km)
	public static double distanceKm(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
			+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
			* Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
	}

	// 중심에서 반경 radiusKm 원을 감싸는 위경도 사각형.
	// 범위는 [-90,90] / [-180,180] 으로 자른다 (날짜변경선 넘김은 다루지 않음 — 서비스 지역이 국내).
	public static BoundingBox boundingBox(double lat, double lng, double radiusKm) {
		double dLat = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
		double cosLat = Math.cos(Math.toRadians(lat));
		double dLng = cosLat < 1e-6 ? 180.0 : Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cosLat));
		return new BoundingBox(
			Math.max(-90.0, lat - dLat), Math.min(90.0, lat + dLat),
			Math.max(-180.0, lng - dLng), Math.min(180.0, lng + dLng));
	}

	public static final class BoundingBox {
		public final double minLat;
		public final double maxLat;
		public final double minLng;
		public final double maxLng;

		public BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {
			this.minLat = minLat;
			this.maxLat = maxLat;
			this.minLng = minLng;
			this.maxLng = maxLng;
		}
	}
}
