package com.ravencore.zero.world

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

enum class DayPhase {
    NIGHT,
    DAWN,
    DAY,
    DUSK
}

object DaylightModel {
    private val transition = Duration.ofMinutes(30)

    fun phase(
        now: Instant,
        zoneId: ZoneId,
        sunrise: Instant,
        sunset: Instant
    ): DayPhase {
        // Retained in the contract because daylight interpretation is
        // explicitly tied to the authoritative device/environment zone.
        require(zoneId.id.isNotBlank())

        val dawnStart = sunrise.minus(transition)
        val dawnEnd = sunrise.plus(transition)
        val duskStart = sunset.minus(transition)
        val duskEnd = sunset.plus(transition)

        return when {
            now.isBefore(dawnStart) -> DayPhase.NIGHT
            now.isBefore(dawnEnd) -> DayPhase.DAWN
            now.isBefore(duskStart) -> DayPhase.DAY
            now.isBefore(duskEnd) -> DayPhase.DUSK
            else -> DayPhase.NIGHT
        }
    }
}
