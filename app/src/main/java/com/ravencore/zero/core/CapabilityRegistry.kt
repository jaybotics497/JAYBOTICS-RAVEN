package com.ravencore.zero.core
enum class Capability { QWEN, STT, TTS, MEMORY, OFFLINE }
class CapabilityRegistry { private val values=mutableMapOf<Capability,Boolean>(); @Synchronized fun set(c:Capability,v:Boolean){values[c]=v}; @Synchronized fun available(c:Capability)=values[c]==true }
