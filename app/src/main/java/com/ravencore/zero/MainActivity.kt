package com.ravencore.zero

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import com.ravencore.zero.app.RavenAppController
import com.ravencore.zero.ui.RavenSceneView

class MainActivity : Activity() {
    private lateinit var controller: RavenAppController
    private lateinit var scene: RavenSceneView
    private var observation: AutoCloseable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = RavenAppController(this)
        scene = RavenSceneView(this)
        setContentView(scene)
        observation = controller.runtime.observe(scene::renderState)
        scene.onTap = ::beginVoiceInteraction
        scene.onLongPress = controller::cancel
    }

    private fun beginVoiceInteraction() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            controller.listen()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_MIC)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MIC && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) controller.listen()
    }

    override fun onDestroy() {
        observation?.close()
        controller.close()
        super.onDestroy()
    }

    companion object { private const val REQUEST_MIC = 7 }
}
