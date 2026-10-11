package com.ravencore.zero.data
import android.content.Context
class RavenPersistence(context:Context){
 private val p=context.getSharedPreferences("raven_state_v1",Context.MODE_PRIVATE)
 fun put(key:String,value:String){require(key.isNotBlank());p.edit().putString(key,value).apply()}
 fun get(key:String):String?=p.getString(key,null)
 fun remove(key:String){p.edit().remove(key).apply()}
 fun snapshot():Map<String,*>=p.all.toMap()
}
