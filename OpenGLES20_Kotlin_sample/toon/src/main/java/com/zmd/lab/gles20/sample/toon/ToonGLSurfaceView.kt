package com.zmd.lab.gles20.sample.toon

import android.content.Context
import android.opengl.GLSurfaceView
import com.zmd.lib.gles20.obj.ObjData

class ToonGLSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        val obj = ObjData("Sphere")
        obj.load(context, "sphere.json")

        val renderer = ToonGLRenderer()
        renderer.obj = obj
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}