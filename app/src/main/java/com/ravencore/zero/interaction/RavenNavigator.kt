package com.ravencore.zero.interaction
import com.ravencore.zero.world.*
class RavenNavigator(initial:WorldState=WorldState()){
 var state:WorldState=initial; private set
 fun moveTo(zone:WorldZone):WorldState { state=state.at(zone); return state }
 fun nearest(nx:Float):WorldZone = WorldZone.entries.minBy { kotlin.math.abs(it.x-nx.coerceIn(0f,1f)) }
}
