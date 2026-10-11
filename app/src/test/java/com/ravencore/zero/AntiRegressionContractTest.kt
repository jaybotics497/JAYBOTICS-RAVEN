package com.ravencore.zero

import com.ravencore.zero.core.AntiRegressionContract
import com.ravencore.zero.raven.RavenPose
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiRegressionContractTest {
    @Test fun oldStaticPresentationMechanismsAreForbidden() {
        assertTrue(AntiRegressionContract.RAVEN_MUST_BE_INDEPENDENT)
        assertTrue(AntiRegressionContract.WORLD_MUST_BE_RAVEN_FREE)
        assertFalse(AntiRegressionContract.SIDE_FILL_ALLOWED)
        assertFalse(AntiRegressionContract.STATIC_WALLPAPER_ALLOWED)
        assertFalse(AntiRegressionContract.HUD_MAY_MUTATE_CORE_STATE)
    }

    @Test fun cameraContractHoldsForBothOrientations() {
        AntiRegressionContract.requireCamera(2400, 1080, RavenPose())
        AntiRegressionContract.requireCamera(1080, 2400, RavenPose())
    }
}
