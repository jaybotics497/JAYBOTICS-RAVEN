package com.ravencore.zero.ai
import java.io.File
import java.security.MessageDigest
data class ModelInfo(val file:File,val sha256:String,val size:Long)
object ModelRegistry { fun inspect(f:File):ModelInfo { require(f.isFile&&f.extension.equals("gguf",true)); val md=MessageDigest.getInstance("SHA-256"); f.inputStream().use{input->val b=ByteArray(1024*1024);while(true){val n=input.read(b);if(n<0)break;md.update(b,0,n)}};return ModelInfo(f,md.digest().joinToString(""){"%02x".format(it)},f.length()) } }
