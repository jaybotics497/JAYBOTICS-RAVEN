package com.ravencore.zero.raven
import android.graphics.*
class RavenRenderer { private val p=Paint(Paint.ANTI_ALIAS_FLAG); private val stroke=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=3f}
 fun draw(c:Canvas,b:RectF,pose:RavenPose){ c.save(); c.translate(0f,b.height()*pose.bodyBob); val cx=b.centerX();
  p.color=Color.rgb(7,12,18); c.drawOval(RectF(b.left+b.width()*.18f,b.top+b.height()*.30f,b.right-b.width()*.08f,b.bottom-b.height()*.08f),p)
  p.color=Color.rgb(11,19,28); val wing=Path().apply{moveTo(cx,b.top+b.height()*.38f);cubicTo(b.right,b.top+b.height()*.42f,b.right,b.top+b.height()*.72f,cx,b.bottom-b.height()*.12f);cubicTo(cx-b.width()*.18f,b.top+b.height()*.70f,cx-b.width()*.12f,b.top+b.height()*.48f,cx,b.top+b.height()*.38f);close()}; c.drawPath(wing,p)
  p.color=Color.rgb(8,15,22); c.drawOval(RectF(b.left+b.width()*.10f,b.top+b.height()*.05f,b.left+b.width()*.63f,b.top+b.height()*.46f),p)
  val beak=Path().apply{moveTo(b.left+b.width()*.19f,b.top+b.height()*.18f);lineTo(b.left-b.width()*.16f,b.top+b.height()*.27f);lineTo(b.left+b.width()*.20f,b.top+b.height()*.31f);close()}; p.color=Color.rgb(15,20,24); c.drawPath(beak,p)
  p.color=Color.rgb(65,170,210); p.alpha=(120+135*pose.eyeIntensity).toInt(); c.drawCircle(b.left+b.width()*.30f,b.top+b.height()*.17f,b.width()*.025f,p); p.alpha=255
  stroke.color=Color.rgb(30,48,62); for(i in 0..5){ val y=b.top+b.height()*(.48f+i*.055f); c.drawLine(cx,y,b.right-b.width()*.12f,y+b.height()*.05f,stroke) }
  p.color=Color.rgb(12,15,16); c.drawRect(cx-b.width()*.04f,b.bottom-b.height()*.14f,cx,b.bottom,p); c.drawRect(cx+b.width()*.08f,b.bottom-b.height()*.14f,cx+b.width()*.12f,b.bottom,p); c.restore() }
}
