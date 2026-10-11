package com.ravencore.zero.ui
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.*
import com.ravencore.zero.camera.SceneCameraV2
import com.ravencore.zero.presentation.*
import com.ravencore.zero.raven.*
import com.ravencore.zero.world.LivingWorldRenderer
class RavenSceneView(context:Context):View(context){ private val world=LivingWorldRenderer(); private val raven=RavenRenderer(); private val adapter=RavenPresentationAdapter(); private var state=RavenState.IDLE; private val hud=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=16f*resources.displayMetrics.scaledDensity}
 var onTap:(()->Unit)?=null
 var onLongPress:(()->Unit)?=null
 private var downAt=0L
 fun renderState(s:RavenState){state=s;invalidate()}
 override fun onDraw(c:Canvas){super.onDraw(c); if(width<=0||height<=0)return; val snap=adapter.snapshot(state,SystemClock.uptimeMillis()); world.draw(c,width,height,snap.world); val f=SceneCameraV2.frame(width,height,snap.pose); raven.draw(c,RectF(f.raven.left,f.raven.top,f.raven.right,f.raven.bottom),snap.pose); drawHud(c,snap); if(isAttachedToWindow)postInvalidateOnAnimation()}
 private fun drawHud(c:Canvas,s:SceneSnapshot){hud.alpha=220;c.drawText("RAVEN  •  ${s.state}",24f,42f,hud); hud.textSize=12f*resources.displayMetrics.scaledDensity;c.drawText("PRIVATE  •  OFFLINE CAPABLE",24f,64f,hud); hud.textSize=16f*resources.displayMetrics.scaledDensity}
 override fun onTouchEvent(e:MotionEvent):Boolean { when(e.action){ MotionEvent.ACTION_DOWN->downAt=SystemClock.uptimeMillis(); MotionEvent.ACTION_UP->{ val held=SystemClock.uptimeMillis()-downAt; performClick(); if(held>=650) onLongPress?.invoke() else onTap?.invoke() } };return true }
 override fun performClick():Boolean{super.performClick();return true}
}
