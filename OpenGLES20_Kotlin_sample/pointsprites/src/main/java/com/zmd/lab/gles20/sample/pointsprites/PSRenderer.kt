package com.zmd.lab.gles20.sample.pointsprites

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.zmd.lib.gles20.camera.GLCamera
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.tan

class PSRenderer(val context: Context): GLSurfaceView.Renderer {
    private val TAG = "PSRenderer"

    private var camera = GLCamera(GLCamera.CAMERA_TYPE_TRACKING)

    private val vertexShaderCode =
        "attribute vec4 aParticle;" +

        "uniform mat4 uMVMatrix;" +
        "uniform mat4 uPMatrix;" +
        "uniform float uPointSize;" +

        "varying float vLifespan;" +

        "void main(void) {" +
            "gl_Position = uPMatrix * uMVMatrix * vec4(aParticle.xyz, 1.0);" +
            "vLifespan = aParticle.w;" +
            "gl_PointSize = uPointSize * vLifespan;" +
        "}"
    private val fragmentShaderCode =
        "precision highp float;" +
        "uniform sampler2D uSampler;" +

        "varying float vLifespan;" +

        "void main(void) {" +
            "vec4 texColor = texture2D(uSampler, gl_PointCoord);" +
            //if (texColor.a == 0.) discard;
            "gl_FragColor = vec4(texColor.rgb, texColor.a * vLifespan);" +
        "}"

    private val mvMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)

    private var angle = 0.0f
    private var cameraElecation = 0.0f
    private var cameraAzimuth = 0.0f

    private var vertexBuffer: FloatBuffer? = null

    private var program = 0

    private var aParticle = 0
    private var uPointSize = 0
    private var uMVMatrix = 0
    private var uPMatrix = 0
    private var uSampler = 0

    private var texNames = IntArray(1)

    private var particleList = ArrayList<Particle>()
    private var particleVertices = FloatArray(1)
    private var particleSize = 40.0f
    private var particleLifeSpan = 3.0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        initProgram()

        camera = GLCamera(GLCamera.CAMERA_TYPE_ORBIT)
        camera.goHome(floatArrayOf(0f, 0f, 5f, 0f))
        //camera.setElevation(45f)

        val assetManager = context.assets
        var bitmap: Bitmap? = null
        try {
            val input = assetManager.open("particle.png")
            bitmap = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        texNames = IntArray(1)
        GLES20.glGenTextures(1, texNames, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0])
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);// x
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        initParticles(1024)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio = width.toFloat() / height.toFloat()

        val near = 0.1f
        val far = 10000.0f

        val a = near * tan(45f * Math.PI / 360f).toFloat()
        val b = ratio * a

        val left = -b
        val right = b
        val bottom = -a
        val top = a

        Matrix.frustumM(pMatrix, 0, left, right, bottom, top, near, far)
    }

    override fun onDrawFrame(gl: GL10?) {
        updateParticles(0.005f)

        //angle += 4f
        cameraAzimuth += 0.51f
        cameraElecation += 0.51f
        camera.setElevation(cameraElecation)
        camera.setAzimuth(cameraAzimuth)

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(100.0f)
        //GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, 0.0f, 0.0f, 0.0f)
        Matrix.rotateM(mvMatrix, 0, angle, 0.0f, 1.0f, 0.0f)

        Matrix.multiplyMM(mvMatrix, 0, camera.getViewM(), 0, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)

        GLES20.glUniform1f(uPointSize, particleSize)

        GLES20.glEnableVertexAttribArray(aParticle)

        GLES20.glVertexAttribPointer(aParticle, 4, GLES20.GL_FLOAT, false, 4 * 4, vertexBuffer)

        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0]);
        //GLES20.glUniform1i(uSampler, 0);


        //GLES20.glDrawElements(GLES20.GL_LINE_LOOP, obj.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        //GLES20.glDrawElements(GLES20.GL_TRIANGLES, obj.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        //GLES20.glDrawElements(GLES20.GL_LINES, sphere.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, particleList.size)

        GLES20.glDisableVertexAttribArray(aParticle)
    }

    private fun initBuffer() {
        // (number of coordinate values * 4 bytes per float)
        val vb = ByteBuffer.allocateDirect(particleVertices.size * 4)
        // use the device hardware's native byte order
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(particleVertices)
        vertexBuffer?.position(0)
    }

    private fun updateBuffer() {
        vertexBuffer?.clear()
        vertexBuffer?.put(particleVertices)
        vertexBuffer?.position(0)
    }

    private fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program);

        aParticle = GLES20.glGetAttribLocation(program, "aParticle")
        uPointSize = GLES20.glGetUniformLocation(program, "uPointSize")
        uPMatrix = GLES20.glGetUniformLocation(program, "uPMatrix")
        uMVMatrix = GLES20.glGetUniformLocation(program, "uMVMatrix")
        uSampler = GLES20.glGetUniformLocation(program, "uSampler")
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun initParticles(count: Int) {
        particleList = ArrayList<Particle>()
        particleVertices = FloatArray(count * 4)

        var p: Particle? = null
        for (i in 0 until count) {
            p = Particle()
            resetParticle(p)
            particleList.add(p)

            particleVertices[(i * 4) + 0] = p.pos[0]
            particleVertices[(i * 4) + 1] = p.pos[1]
            particleVertices[(i * 4) + 2] = p.pos[2]
            particleVertices[(i * 4) + 3] = p.remainingLife / p.lifeSpan
        }

        initBuffer()
    }

    private fun resetParticle(p: Particle) {
        p.pos = floatArrayOf(0.0f, 0.0f, 0.0f)
        p.vel = floatArrayOf(
            (Math.random().toFloat() * 20.0f) - 10.0f,
            (Math.random().toFloat() * 20.0f),
            (Math.random().toFloat() * 20.0f) - 10.0f
        )

        p.lifeSpan = (Math.random().toFloat() * particleLifeSpan)
        p.remainingLife = p.lifeSpan
    }

    private fun updateParticles(elapsed: Float) {
        var count = particleList.size
        var p: Particle? = null
        for(i in 0 until count) {
            p = particleList[i]

            p.remainingLife -= elapsed
            if(p.remainingLife <= 0) {
                resetParticle(p)
            }

            p.pos[0] += p.vel[0] * elapsed
            p.pos[1] += p.vel[1] * elapsed
            p.pos[2] += p.vel[2] * elapsed

            p.vel[1] -= 9.8f * elapsed
            if(p.pos[1] < 0) {
                p.vel[1] *= -0.75f
                p.pos[1] = 0f
            }

            particleVertices[(i * 4) + 0] = p.pos[0]
            particleVertices[(i * 4) + 1] = p.pos[1]
            particleVertices[(i * 4) + 2] = p.pos[2]
            particleVertices[(i * 4) + 3] = p.remainingLife / p.lifeSpan
        }

        updateBuffer()
        //initBuffer();
    }

    class Particle {
        var pos = FloatArray(3)
        var vel = FloatArray(3)
        var lifeSpan = 0f
        var remainingLife = 0f
    }
}