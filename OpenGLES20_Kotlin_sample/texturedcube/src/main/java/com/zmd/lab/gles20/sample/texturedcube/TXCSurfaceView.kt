package com.zmd.lab.gles20.sample.texturedcube

import android.content.Context
import android.opengl.GLSurfaceView
import com.zmd.lib.gles20.obj.ObjData

class TXCSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val obj = ObjData("Cube")
        obj.load(context, "complexCube.json")

        val renderer = TXCRenderer(context)
        renderer.obj = obj
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}