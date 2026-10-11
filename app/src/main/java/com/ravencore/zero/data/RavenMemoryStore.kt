package com.ravencore.zero.data
import android.content.Context
class RavenMemoryStore(c:Context){ private val p=c.getSharedPreferences("raven_memory_v1",Context.MODE_PRIVATE)
 fun remember(key:String,value:String)=p.edit().putString(key,value).commit(); fun recall(key:String)=p.getString(key,null); fun all(): Map<String, *> = p.all }
