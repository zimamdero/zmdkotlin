package com.zmd.lab.gles20.sample.postprocessing

import android.content.Context
import android.opengl.GLSurfaceView

class GL3DObjsSurfaceView(context: Context): GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)

        val sphereRender = ObjPhongRender(context, "Sphere", "sphere.json")
        sphereRender.place(2.0f, 1.0f, 0.0f)
        //sphereRender.setType(GLES20.GL_LINES);
        sphereRender.setScale(2.0f, 2.0f, 2.0f)

        val cubeRender = ObjPhongRender(context, "ComplextCube", "complexCube.json")
        cubeRender.place(-2.0f, 1.0f, 0.0f)
        cubeRender.setMaterialAmbient(0.8f, 0.6f, 0.5f, 1.0f)
        cubeRender.setMaterialDiffuse(0.8f, 0.1f, 0.3f, 1.0f)
        //cubeRender.rotate(45f, -45f, 0.0f);

        val flagRender = ObjPhongRender(context, "Flag", "flag.json")
        //flagRender.setType(GLES20.GL_LINES);
        flagRender.place(0.0f, 0.0f, 0.0f)
        flagRender.setMaterialAmbient(0.5f, 0.6f, 0.8f, 1.0f)
        flagRender.setMaterialDiffuse(0.3f, 0.1f, 0.8f, 1.0f)
        flagRender.setScale(0.2f, 0.2f, 0.2f)

        val planeRender = ObjPhongRender(context, "Plane", "plane.json")
        //planeRender.place(0.0f, 0.0f, -190f);
        planeRender.place(0f, 5f, 0f)
        //planeRender.rotate(90f, 0.0f, 0.0f);
        planeRender.setScale(0.1f, 1.0f, 1.0f)
        planeRender.setMaterialDiffuse(0.5f, 0.6f, 0.5f, 1.0f)

        val coneRender = ObjPhongRender(context, "Cone", "cone.json")
        coneRender.place(-1.0f, 0.0f, -1.5f)
        coneRender.setMaterialDiffuse(0.6f, 0.6f, 0.9f, 1.0f)
        coneRender.setScale(0.3f, 0.3f, 0.3f)

        val wallRender = ObjPhongRender(context, "Wall", "wall.json")
        wallRender.place(0.0f, 0.0f, -4.0f)
        wallRender.setScale(0.03f, 0.5f, 1.0f)
        wallRender.setMaterialDiffuse(0.9f, 0.7f, 0.6f, 1.0f)

        //val renderer = GLObjsRenderer()
        //val renderer = PostBlurRenderer()
        //val renderer = PostFilmgrainRenderer(context)
        val renderer = PostGrayScaleRenderer()
        //val renderer = PostInvertRenderer()
        //val renderer = PostWavyRenderer()

        renderer.addObj(sphereRender)
        renderer.addObj(cubeRender)
        renderer.addObj(flagRender)
        renderer.addObj(planeRender)
        renderer.addObj(coneRender)
        renderer.addObj(wallRender)

        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
}