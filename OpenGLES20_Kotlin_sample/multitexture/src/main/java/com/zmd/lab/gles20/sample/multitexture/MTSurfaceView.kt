package com.zmd.lab.gles20.sample.multitexture

import android.content.Context
import android.opengl.GLSurfaceView
import com.zmd.lib.gles20.obj.ObjData

class MTSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val obj = ObjData("Cube")
        obj.load(context, "complexCube.json")

        val renderer = MTRenderer(context)
        renderer.obj = obj
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}