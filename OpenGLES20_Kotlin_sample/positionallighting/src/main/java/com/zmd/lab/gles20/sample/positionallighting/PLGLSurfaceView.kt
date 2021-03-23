package com.zmd.lab.gles20.sample.positionallighting

import android.content.Context
import android.opengl.GLSurfaceView
import com.zmd.lib.gles20.obj.ObjData

class PLGLSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val sphere = ObjData("Sphere")
        sphere.load(context, "sphere.json")

        val renderer = PLGLRenderer()
        renderer.obj = sphere
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}