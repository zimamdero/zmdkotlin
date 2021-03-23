package com.zmd.lab.gles20.sample.postprocessing;

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.zmd.lib.gles20.camera.GLCamera
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.tan

class GLObjsRenderer(): GLSurfaceView.Renderer {
    private var camera = GLCamera(GLCamera.CAMERA_TYPE_TRACKING)
    private val pMatrix = FloatArray(16)
    private val objList: ArrayList<ObjPhongRender> = ArrayList()
    private var azimuth = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        camera = GLCamera(GLCamera.CAMERA_TYPE_ORBIT)
        camera.goHome(floatArrayOf(0f, 2f, 14f, 0f))

        for (obj in objList) {
            obj.initProgram()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()

        val near = 0.1f
        val far = 10000.0f

        val a = near * tan(45f * Math.PI / 360f).toFloat()
        val b = ratio * a

        val left = -b
        val bottom = -a

        Matrix.frustumM(pMatrix, 0, left, b, bottom, a, near, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(100.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        azimuth += 0.5f
        camera.setAzimuth(azimuth)

        for (obj in objList) {
            obj.draw(pMatrix, camera)
        }
    }

    fun addObj(obj: ObjPhongRender) {
        objList.add(obj)
    }
}
