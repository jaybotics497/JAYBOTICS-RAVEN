package com.ravencore.zero.raven
import com.ravencore.zero.core.CoreEvent
class RavenStateMachine(initial:RavenState=RavenState.IDLE) { var state=initial; private set
 fun accept(e:CoreEvent):RavenState { state=when(e){
  CoreEvent.SttStarted->RavenState.LISTENING; CoreEvent.SttStopped->if(state==RavenState.LISTENING)RavenState.IDLE else state
  CoreEvent.InferenceStarted->RavenState.THINKING; CoreEvent.GenerationStarted->RavenState.GENERATING
  CoreEvent.GenerationCompleted->RavenState.ACTIVE; CoreEvent.GenerationCancelled->RavenState.IDLE
  CoreEvent.TtsStarted->RavenState.SPEAKING; CoreEvent.TtsCompleted,CoreEvent.TtsCancelled->RavenState.IDLE
  CoreEvent.TaskCompleted->RavenState.SUCCESS; CoreEvent.ServiceUnavailable->RavenState.OFFLINE
  is CoreEvent.Fault->RavenState.ERROR; CoreEvent.RecoveryRequested->RavenState.IDLE }; return state }
}
