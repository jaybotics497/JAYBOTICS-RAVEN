package com.ravencore.zero.world

enum class WorldZone(val x:Float,val y:Float){
 LEFT_LOOKOUT(.12f,.73f), FIELD_CENTER(.35f,.76f), WATER_EDGE(.53f,.78f), WATERFALL(.47f,.58f),
 TAKEOFF_POINT(.67f,.72f), HOME_PERCH(.72f,.62f), RIGHT_LOOKOUT(.91f,.68f)
}
data class WorldState(val ravenX:Float=.72f,val ravenY:Float=.62f,val zone:WorldZone=WorldZone.HOME_PERCH,val timeMs:Long=0L){
 init { require(ravenX in 0f..1f && ravenY in 0f..1f) }
 fun at(z:WorldZone)=copy(ravenX=z.x,ravenY=z.y,zone=z)
}
