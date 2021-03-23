package com.zmd.lib.gles20.sample.normalmap

import android.content.Context
import android.opengl.GLSurfaceView

class NMSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val obj = NormalObjData("Cube")
        obj.load(context, "complexCube.json")

        val renderer = NMRenderer(context)
        renderer.obj = obj
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}