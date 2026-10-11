package com.ravencore.zero.core
import com.ravencore.zero.raven.*
import java.util.concurrent.CopyOnWriteArraySet
class RavenRuntime { private val machine=RavenStateMachine(); private val observers=CopyOnWriteArraySet<(RavenState)->Unit>(); val state get()=machine.state
 fun observe(o:(RavenState)->Unit):AutoCloseable { observers+=o; o(state); return AutoCloseable{observers-=o} }
 @Synchronized fun dispatch(e:CoreEvent){ val s=machine.accept(e); observers.forEach{it(s)} }
}
