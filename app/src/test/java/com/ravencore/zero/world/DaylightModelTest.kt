package com.ravencore.zero.world

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class DaylightModelTest {

    private val zone = ZoneId.of("Europe/Dublin")
    private val sunrise = Instant.parse("2026-10-11T06:45:00Z")
    private val sunset = Instant.parse("2026-10-11T17:45:00Z")

    @Test
    fun beforeDawnIsNight() {
        assertEquals(
            DayPhase.NIGHT,
            DaylightModel.phase(
                Instant.parse("2026-10-11T05:30:00Z"),
                zone,
                sunrise,
                sunset
            )
        )
    }

    @Test
    fun sunriseTransitionIsDawn() {
        assertEquals(
            DayPhase.DAWN,
            DaylightModel.phase(
                Instant.parse("2026-10-11T06:35:00Z"),
                zone,
                sunrise,
                sunset
            )
        )
    }

    @Test
    fun middleOfDayIsDay() {
        assertEquals(
            DayPhase.DAY,
            DaylightModel.phase(
                Instant.parse("2026-10-11T12:00:00Z"),
                zone,
                sunrise,
                sunset
            )
        )
    }

    @Test
    fun sunsetTransitionIsDusk() {
        assertEquals(
            DayPhase.DUSK,
            DaylightModel.phase(
                Instant.parse("2026-10-11T17:55:00Z"),
                zone,
                sunrise,
                sunset
            )
        )
    }

    @Test
    fun afterDuskIsNight() {
        assertEquals(
            DayPhase.NIGHT,
            DaylightModel.phase(
                Instant.parse("2026-10-11T19:00:00Z"),
                zone,
                sunrise,
                sunset
            )
        )
    }
}
