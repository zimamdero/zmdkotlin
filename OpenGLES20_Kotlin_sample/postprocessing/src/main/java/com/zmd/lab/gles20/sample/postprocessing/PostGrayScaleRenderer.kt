package com.zmd.lab.gles20.sample.postprocessing

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.zmd.lib.gles20.camera.GLCamera
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PostGrayScaleRenderer: GLSurfaceView.Renderer {
    private val vertexShaderCode =
            "attribute vec2 aVertexPosition;" +
            "attribute vec2 aVertexTextureCoords;" +
            "varying vec2 vTextureCoord;" +
            "void main(void) {" +
                "vTextureCoord = aVertexTextureCoords;" +
                "gl_Position = vec4(aVertexPosition, 0.0, 1.0);" +
            "}"
    private val fragmentShaderCode =
                // gray scale
                "precision highp float;" +
                "uniform sampler2D uSampler;" +
                "varying vec2 vTextureCoord;" +
                "void main(void)" +
                "{" +
                    "vec4 frameColor = texture2D(uSampler, vTextureCoord);" +
                    "float luminance = frameColor.r * 0.3 + frameColor.g * 0.59 + frameColor.b * 0.11;" +
                    "gl_FragColor = vec4(luminance, luminance, luminance, frameColor.a);" +
                "}"

    private val vertices = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f, 1.0f,
        -1.0f, 1.0f,
        1.0f, -1.0f,
        1.0f, 1.0f
    )

    private val textureCoords = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        0.0f, 1.0f,
        1.0f, 0.0f,
        1.0f, 1.0f
    )

    private var vertexBuffer: FloatBuffer? = null
    private var textureCoordsBuffer: FloatBuffer? = null
    private var program = 0

    private var aVertexPosition = 0
    private var aVertexTextureCoords = 0
    private var uSampler = 0

    private var camera = GLCamera(GLCamera.CAMERA_TYPE_TRACKING)
    private val pMatrix = FloatArray(16)

    private var objList: ArrayList<ObjPhongRender> = ArrayList()

    private var width = 0
    private var height = 0

    private var texNames = IntArray(1)
    private var renderBuffer = IntArray(1)
    private var frameBuffer =  IntArray(1)

    private var azimuth = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        initBuffer()
        initProgram()

        camera = GLCamera(GLCamera.CAMERA_TYPE_ORBIT)
        camera.goHome(floatArrayOf(0f, 2f, 14f, 0f))

        for (obj in objList) {
            obj.initProgram()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        this.width = width
        this.height = height

        val ratio = width.toFloat() / height

        val near = 0.1f
        val far = 10000.0f

        val a = near * Math.tan(45f * Math.PI / 360f).toFloat()
        val b = ratio * a

        val left = -b
        val bottom = -a

        Matrix.frustumM(pMatrix, 0, left, b, bottom, a, near, far)

        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        GLES20.glEnable(GLES20.GL_RENDERBUFFER)
        GLES20.glEnable(GLES20.GL_FRAMEBUFFER)

        texNames = IntArray(1)
        GLES20.glGenTextures(1, texNames, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexParameterf(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, this.width, this.height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )

        renderBuffer = IntArray(1)
        frameBuffer = IntArray(1)
        GLES20.glGenRenderbuffers(1, renderBuffer, 0)
        GLES20.glGenFramebuffers(1, frameBuffer, 0)

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer[0])
        GLES20.glRenderbufferStorage(
            GLES20.GL_RENDERBUFFER,
            GLES20.GL_DEPTH_COMPONENT16,
            this.width,
            this.height
        )

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, texNames[0], 0
        )
        GLES20.glFramebufferRenderbuffer(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER,
            renderBuffer[0]
        )

        // clean up
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        //GLES20.glClearDepthf(100.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        //GLES20.glDepthFunc(GLES20.GL_LEQUAL);
        azimuth += 0.5f
        camera.setAzimuth(azimuth)

        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, renderBuffer[0]);
        //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        for (obj in objList) {
            obj.draw(pMatrix, camera)
        }

        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)

        // plane ...
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUseProgram(program)

        GLES20.glEnableVertexAttribArray(aVertexPosition)

        GLES20.glVertexAttribPointer(
            aVertexPosition, 2,
            GLES20.GL_FLOAT, false,
            2 * 4, vertexBuffer
        )

        GLES20.glEnableVertexAttribArray(aVertexTextureCoords)

        GLES20.glVertexAttribPointer(
            aVertexTextureCoords, 2,
            GLES20.GL_FLOAT, false,
            2 * 4, textureCoordsBuffer
        )

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0])
        GLES20.glUniform1i(uSampler, 0)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)

        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        //GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
        //GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private fun initBuffer() {
        // (number of coordinate values * 4 bytes per float)
        val vb = ByteBuffer.allocateDirect(vertices.size * 4)
        // use the device hardware's native byte order
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(vertices)
        vertexBuffer?.position(0)
        val tcb = ByteBuffer.allocateDirect(textureCoords.size * 4)
        tcb.order(ByteOrder.nativeOrder())
        textureCoordsBuffer = tcb.asFloatBuffer()
        textureCoordsBuffer?.put(textureCoords)
        textureCoordsBuffer?.position(0)
    }

    fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        aVertexPosition = GLES20.glGetAttribLocation(program, "aVertexPosition")
        aVertexTextureCoords = GLES20.glGetAttribLocation(program, "aVertexTextureCoords")
        uSampler = GLES20.glGetUniformLocation(program, "uSampler")
    }

    fun loadShader(type: Int, shaderCode: String?): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun addObj(obj: ObjPhongRender) {
        objList.add(obj)
    }
}