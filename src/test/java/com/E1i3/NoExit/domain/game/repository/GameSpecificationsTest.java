package com.E1i3.NoExit.domain.game.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GameSpecificationsTest {

	@Test
	void escape_likeWildcards() {
		assertEquals("100!%", GameSpecifications.escape("100%"));
		assertEquals("a!_b", GameSpecifications.escape("a_b"));
		assertEquals("!!", GameSpecifications.escape("!"));
		assertEquals("강남구", GameSpecifications.escape("강남구"));
	}
}
