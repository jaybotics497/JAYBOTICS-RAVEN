package com.ravencore.zero.camera

import com.ravencore.zero.raven.RavenPose

data class CameraRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun width(): Float = right - left
    fun height(): Float = bottom - top
    fun centerX(): Float = (left + right) * 0.5f
}

data class CameraFrame(
    val raven: CameraRect,
    val scale: Float,
    val portrait: Boolean
)

object SceneCameraV2 {
    fun frame(w: Int, h: Int, pose: RavenPose): CameraFrame {
        require(w > 0 && h > 0)

        val portrait = h > w
        val target = if (portrait) .60f else .53f

        val rh = h * target
        val rw = rh * .62f
        val ax = if (portrait) .55f else .68f
        val ay = if (portrait) .43f else .45f

        val side = w * .08f
        val head = h * .12f
        val bottom = h * .08f

        var left = w * ax - rw * .5f
        var top = h * ay - rh * .32f

        left = left.coerceIn(side, w - side - rw)
        top = top.coerceIn(head, h - bottom - rh)

        return CameraFrame(
            raven = CameraRect(left, top, left + rw, top + rh),
            scale = rh / 1000f,
            portrait = portrait
        )
    }

    fun accepts(w: Int, h: Int, f: CameraFrame): Boolean {
        val r = f.raven
        val frac = r.height() / h
        return frac <= .75f &&
            r.top >= h * .12f &&
            r.bottom <= h * .92f &&
            r.left >= w * .08f &&
            r.right <= w * .92f
    }
}
