package com.zmd.lab.gles20.sample.ambient.goraud.phong

import android.content.Context
import android.opengl.GLSurfaceView
import com.zmd.lib.gles20.obj.ObjData

class AmGLSurfaceView(context: Context) : GLSurfaceView(context) {
    private val renderer: AmGLRenderer

    init {
        setEGLContextClientVersion(2)

        val sphere = ObjData("Sphere")
        sphere.load(context, "sphere.json")

        renderer = AmGLRenderer()
        renderer.sphere = sphere

        setRenderer(renderer)

        //renderMode = RENDERMODE_WHEN_DIRTY
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}