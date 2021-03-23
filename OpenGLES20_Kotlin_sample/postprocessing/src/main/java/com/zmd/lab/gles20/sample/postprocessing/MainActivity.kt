package com.zmd.lab.gles20.sample.postprocessing

import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var glView: GLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glView = GL3DObjsSurfaceView(this)
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