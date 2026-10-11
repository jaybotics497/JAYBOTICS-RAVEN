package com.ravencore.zero.core
import java.util.concurrent.atomic.AtomicLong
class OperationEpoch { private val epoch=AtomicLong(); fun next()=epoch.incrementAndGet(); fun current()=epoch.get(); fun owns(v:Long)=epoch.get()==v; fun cancelAll()=epoch.incrementAndGet() }
