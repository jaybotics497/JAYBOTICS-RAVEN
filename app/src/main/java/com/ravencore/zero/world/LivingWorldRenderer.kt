package com.ravencore.zero.world
import android.graphics.*
import com.ravencore.zero.presentation.WorldState
class LivingWorldRenderer { private val p=Paint(Paint.ANTI_ALIAS_FLAG)
 fun draw(c:Canvas,w:Int,h:Int,s:WorldState){
  p.shader=LinearGradient(0f,0f,w.toFloat(),0f,intArrayOf(Color.rgb(244,137,67),Color.rgb(63,111,147),Color.rgb(10,35,67)),null,Shader.TileMode.CLAMP); c.drawRect(0f,0f,w.toFloat(),h.toFloat(),p); p.shader=null
  p.color=Color.rgb(38,63,72); val mountain=Path().apply{moveTo(0f,h*.56f);lineTo(w*.18f,h*.25f);lineTo(w*.34f,h*.56f);lineTo(w*.48f,h*.20f);lineTo(w*.67f,h*.56f);lineTo(w.toFloat(),h*.40f);lineTo(w.toFloat(),h.toFloat());lineTo(0f,h.toFloat());close()}; c.drawPath(mountain,p)
  p.color=Color.rgb(24,50,47); c.drawRect(0f,h*.55f,w.toFloat(),h.toFloat(),p)
  p.color=Color.rgb(124,205,225); c.drawRect(w*.45f,h*.38f,w*.485f,h*.72f,p); p.color=Color.rgb(55,116,135); val river=Path().apply{moveTo(w*.46f,h*.69f);lineTo(w*.78f,h.toFloat());lineTo(w*.34f,h.toFloat());close()}; c.drawPath(river,p)
  p.color=Color.rgb(16,38,32); for(i in 0..18){ val x=w*(i/18f); val th=h*(.18f+(i%4)*.025f); c.drawRect(x,h*.60f-th*.15f,x+w*.025f,h*.84f,p) }
  p.color=Color.rgb(50,55,49); c.drawOval(RectF(w*.61f,h*.72f,w*.82f,h*.86f),p)
 }
}
