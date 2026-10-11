package com.ravencore.zero.core

import com.ravencore.zero.camera.CameraFrame
import com.ravencore.zero.camera.SceneCameraV2
import com.ravencore.zero.raven.RavenPose

/** Hard failures derived from the last pre-fresh-build Jaybotics screenshots. */
object AntiRegressionContract {
    const val RAVEN_MUST_BE_INDEPENDENT = true
    const val WORLD_MUST_BE_RAVEN_FREE = true
    const val SIDE_FILL_ALLOWED = false
    const val STATIC_WALLPAPER_ALLOWED = false
    const val HUD_MAY_MUTATE_CORE_STATE = false

    fun requireCamera(width: Int, height: Int, pose: RavenPose, frame: CameraFrame = SceneCameraV2.frame(width, height, pose)) {
        require(SceneCameraV2.accepts(width, height, frame)) { "Scene Camera v2 acceptance failed" }
        val fraction = frame.raven.height() / height
        if (height > width) require(fraction in .58f.. .63f) { "Portrait Raven must occupy 58-63% viewport height" }
        else require(fraction in .50f.. .55f) { "Landscape Raven must occupy 50-55% viewport height" }
    }
}
