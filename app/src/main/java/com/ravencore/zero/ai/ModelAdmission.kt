package com.ravencore.zero.ai
import java.io.File
import java.security.MessageDigest

data class ModelAdmission(val file:File,val sha256:String,val size:Long)
object ModelVerifier {
 fun sha256(file:File):String { val md=MessageDigest.getInstance("SHA-256"); file.inputStream().use { input -> val b=ByteArray(1024*1024); while(true){ val n=input.read(b); if(n<0)break; md.update(b,0,n) } }; return md.digest().joinToString(""){"%02x".format(it)} }
 fun admit(file:File,expectedSha:String?=null):ModelAdmission { require(file.isFile && file.length()>0){"GGUF model missing"}; require(file.extension.equals("gguf",true)){"Model must be GGUF"}; val hash=sha256(file); if(expectedSha!=null) require(hash.equals(expectedSha,true)){"Model SHA-256 mismatch"}; return ModelAdmission(file,hash,file.length()) }
}
