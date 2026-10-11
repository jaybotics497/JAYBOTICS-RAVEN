package com.ravencore.zero.world

import java.time.Instant
import java.time.ZoneId

enum class WeatherFreshness {
    FRESH,
    STALE
}

data class EnvironmentFacts(
    val observedAt: Instant,
    val expiresAt: Instant?,
    val zoneId: ZoneId,
    val source: String
) {
    fun freshness(now: Instant): WeatherFreshness =
        if (expiresAt == null || !now.isAfter(expiresAt)) {
            WeatherFreshness.FRESH
        } else {
            WeatherFreshness.STALE
        }
}
