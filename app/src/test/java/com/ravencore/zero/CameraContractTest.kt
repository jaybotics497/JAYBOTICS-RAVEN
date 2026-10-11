package com.ravencore.zero
import com.ravencore.zero.camera.SceneCameraV2
import com.ravencore.zero.raven.RavenPose
import org.junit.Assert.*
import org.junit.Test
class CameraContractTest { @Test fun landscape(){val f=SceneCameraV2.frame(1920,1080,RavenPose());assertEquals(.53f,f.raven.height()/1080f,.001f);assertTrue(SceneCameraV2.accepts(1920,1080,f))}; @Test fun portrait(){val f=SceneCameraV2.frame(1080,1920,RavenPose());assertEquals(.60f,f.raven.height()/1920f,.001f);assertTrue(SceneCameraV2.accepts(1080,1920,f))} }
