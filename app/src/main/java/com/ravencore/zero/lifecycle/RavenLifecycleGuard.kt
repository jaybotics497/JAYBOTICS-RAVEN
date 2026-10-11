package com.ravencore.zero.lifecycle
import java.util.concurrent.atomic.AtomicBoolean
class RavenLifecycleGuard { private val destroyed=AtomicBoolean(false); fun destroy(){destroyed.set(true)}; fun requireAlive(){check(!destroyed.get()){"Raven lifecycle already destroyed"}}; val isDestroyed get()=destroyed.get() }
