package com.ravencore.zero.presentation
import com.ravencore.zero.raven.*
data class WorldState(val environmentLevel:Float=.55f,val wind:Float=.15f,val time:Float=.72f)
data class SceneSnapshot(val state:RavenState,val pose:RavenPose,val world:WorldState)
