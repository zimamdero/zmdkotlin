package com.zmd.lab.gles20.sample.pointsprites

import android.content.Context
import android.opengl.GLSurfaceView

class PSSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val renderer = PSRenderer(context)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}