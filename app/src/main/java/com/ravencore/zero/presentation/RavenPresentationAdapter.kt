package com.ravencore.zero.presentation
import com.ravencore.zero.raven.*
class RavenPresentationAdapter(private val engine:RavenAnimationEngine=RavenAnimationEngine()) { fun snapshot(state:RavenState, now:Long, amp:Float=0f)=SceneSnapshot(state,engine.pose(state,now,amp),WorldState()) }
