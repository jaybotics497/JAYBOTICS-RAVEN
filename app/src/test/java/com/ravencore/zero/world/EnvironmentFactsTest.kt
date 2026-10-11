package com.ravencore.zero.world

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class EnvironmentFactsTest {

    @Test
    fun freshObservationIsFreshBeforeExpiry() {
        val facts = EnvironmentFacts(
            observedAt = Instant.parse("2026-10-11T11:00:00Z"),
            expiresAt = Instant.parse("2026-10-11T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Dublin"),
            source = "test"
        )

        assertEquals(
            WeatherFreshness.FRESH,
            facts.freshness(Instant.parse("2026-10-11T11:30:00Z"))
        )
    }

    @Test
    fun expiredObservationIsStale() {
        val facts = EnvironmentFacts(
            observedAt = Instant.parse("2026-10-11T11:00:00Z"),
            expiresAt = Instant.parse("2026-10-11T12:00:00Z"),
            zoneId = ZoneId.of("Europe/Dublin"),
            source = "test"
        )

        assertEquals(
            WeatherFreshness.STALE,
            facts.freshness(Instant.parse("2026-10-11T12:00:01Z"))
        )
    }
}
