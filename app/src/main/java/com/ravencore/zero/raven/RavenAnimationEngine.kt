package com.ravencore.zero.raven
import kotlin.math.sin
class RavenAnimationEngine { fun pose(state:RavenState, nowMs:Long, amp:Float=0f):RavenPose { val t=nowMs/1000f; val bob=sin(t*1.7f)*.006f
 val eye=when(state){RavenState.SLEEPING->0f else->1f}; val intensity=when(state){RavenState.LISTENING,RavenState.ERROR->1f; RavenState.THINKING,RavenState.GENERATING->.88f; else->.65f}
 return RavenPose(bodyBob=bob,headTurn=if(state==RavenState.THINKING)sin(t*.8f)*4f else 0f,headTilt=if(state==RavenState.INSPECTING)8f else 0f,wingOpen=if(state==RavenState.ALERT).08f else 0f,beakOpen=if(state==RavenState.SPEAKING)amp.coerceIn(0f,.3f) else 0f,eyeOpen=eye,eyeIntensity=intensity) } }
