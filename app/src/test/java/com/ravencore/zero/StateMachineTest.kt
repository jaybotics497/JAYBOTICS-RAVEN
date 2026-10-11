package com.ravencore.zero
import com.ravencore.zero.core.*
import com.ravencore.zero.raven.*
import org.junit.Assert.*
import org.junit.Test
class StateMachineTest { @Test fun coreFlow(){val s=RavenStateMachine();assertEquals(RavenState.LISTENING,s.accept(CoreEvent.SttStarted));assertEquals(RavenState.THINKING,s.accept(CoreEvent.InferenceStarted));assertEquals(RavenState.GENERATING,s.accept(CoreEvent.GenerationStarted));assertEquals(RavenState.ACTIVE,s.accept(CoreEvent.GenerationCompleted))} }
