package com.zmd.lab.gles20.sample.cubemap

import android.content.Context
import android.opengl.GLSurfaceView

import com.zmd.lib.gles20.obj.ObjData

class CMSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        val cubemap = ObjData("cubemap")
        cubemap.load(context, "complexCube.json")
        val renderer = CMRenderer(context)
        renderer.obj = cubemap
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}