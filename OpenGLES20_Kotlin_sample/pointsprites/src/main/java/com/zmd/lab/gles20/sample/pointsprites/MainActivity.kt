package com.zmd.lab.gles20.sample.pointsprites

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private var glView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = PSSurfaceView(this)
        setContentView(glView)
    }

    override fun onPause() {
        super.onPause()
        glView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
    }
}