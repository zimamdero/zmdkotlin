package com.zmd.lab.gles20.sample.positionallighting

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.zmd.lib.gles20.obj.ObjData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.tan

class PLGLRenderer: GLSurfaceView.Renderer {
    private val TAG = "PositionalLightingGLRenderer"

    var obj = ObjData("sphere")
        set(value) {
            field = value
            initBuffer()
        }

    private val vertexShaderCode =
        "attribute vec3 aVertexPosition;" +
        "attribute vec3 aVertexNormal;" +

        "uniform mat4 uMVMatrix;" +
        "uniform mat4 uPMatrix;" +
        "uniform mat4 uNMatrix;" +

        "uniform vec3 uLightPosition;" +

        "varying vec3 vNormal;" +
        "varying vec3 vLightRay;" +
        "varying vec3 vEyeVec;" +

        "void main(void) {" +

            //Transformed vertex position
            "vec4 vertex = uMVMatrix * vec4(aVertexPosition, 1.0);" +

            //Transformed normal position
            "vNormal = vec3(uNMatrix * vec4(aVertexNormal, 1.0));" +

            //Transformed light position
            "vec4 light = uMVMatrix * vec4(uLightPosition,1.0);" +

            //Light position
            "vLightRay = vertex.xyz-light.xyz;" +

            //Vector Eye
            "vEyeVec = -vec3(vertex.xyz);" +

            //Final vertex position
            "gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);" +

        "}"
    private val fragmentShaderCode =
        "precision highp float;" +

        "uniform vec4 uLightAmbient;" +
        "uniform vec4 uLightDiffuse;" +
        "uniform vec4 uLightSpecular;" +

        "uniform vec4 uMaterialAmbient;" +
        "uniform vec4 uMaterialDiffuse;" +
        "uniform vec4 uMaterialSpecular;" +
        "uniform float uShininess;" +

        "varying vec3 vNormal;" +
        "varying vec3 vLightRay;" +
        "varying vec3 vEyeVec;" +

        "void main(void)" +
        "{" +

            "vec3 L = normalize(vLightRay);" +
            "vec3 N = normalize(vNormal);" +

            //Lambert's cosine law
            "float lambertTerm = dot(N,-L);" +

            //Ambient Term
            "vec4 Ia = uLightAmbient * uMaterialAmbient;" +

            //Diffuse Term
            "vec4 Id = vec4(0.0,0.0,0.0,1.0);" +

            //Specular Term
            "vec4 Is = vec4(0.0,0.0,0.0,1.0);" +

            "if(lambertTerm > 0.0)" +
            "{" +
            "Id = uLightDiffuse * uMaterialDiffuse * lambertTerm;" +
            "vec3 E = normalize(vEyeVec);" +
            "vec3 R = reflect(L, N);" +
            "float specular = pow( max(dot(R, E), 0.0), uShininess);" +
            "Is = uLightSpecular * uMaterialSpecular * specular;" +
            "}" +

            //Final color
            "vec4 finalColor = Ia + Id + Is;" +
            "finalColor.a = 1.0;" +

            "gl_FragColor = finalColor;" +

        "}"

    private val mvMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val nMatrix = FloatArray(16)

    private var angle = 0.0f
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var normalsBuffer: FloatBuffer? = null
    private var program = 0

    private val COORDS_PER_VERTEX = 3
    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private var aVertexPosition = 0
    private var aVertexNormal = 0
    private var uPMatrix = 0
    private var uMVMatrix = 0
    private var uNMatrix = 0
    private var uMaterialAmbient = 0
    private var uMaterialDiffuse = 0
    private var uMaterialSpecular = 0
    private var uShininess = 0
    private var uLightAmbient = 0
    private var uLightDiffuse = 0
    private var uLightSpecular = 0
    private var uLightPosition = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initProgram()
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
        //angle += 1f

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(100.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, 0.0f, 0.0f, -3.5f)
        Matrix.rotateM(mvMatrix, 0, angle, 0.0f, 1.0f, 0.0f)

        GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)

        Matrix.transposeM(nMatrix, 0, mvMatrix, 0)
        Matrix.invertM(nMatrix, 0, nMatrix, 0)

        GLES20.glUniformMatrix4fv(uNMatrix, 1, false, nMatrix, 0)

        GLES20.glEnableVertexAttribArray(aVertexPosition)
        GLES20.glEnableVertexAttribArray(aVertexNormal)

        GLES20.glVertexAttribPointer(aVertexPosition, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer)

        GLES20.glVertexAttribPointer(aVertexNormal, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, normalsBuffer)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, obj.indices.size,
            GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        //GLES20.glDrawElements(GLES20.GL_LINES, sphere.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aVertexPosition)
        GLES20.glDisableVertexAttribArray(aVertexNormal)
    }

    private fun initBuffer() {
        // (number of coordinate values * 4 bytes per float)
        val vb = ByteBuffer.allocateDirect(obj.vertices.size * 4)
        // use the device hardware's native byte order
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(obj.vertices)
        vertexBuffer?.position(0)

        // (# of coordinate values * 2 bytes per short)
        val ib = ByteBuffer.allocateDirect(obj.indices.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(obj.indices)
        indexBuffer?.position(0)

        val nb = ByteBuffer.allocateDirect(obj.normals.size * 4)
        nb.order(ByteOrder.nativeOrder())
        normalsBuffer = nb.asFloatBuffer()
        normalsBuffer?.put(obj.normals)
        normalsBuffer?.position(0)
    }

    private fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        GLES20.glUseProgram(program)

        aVertexPosition = GLES20.glGetAttribLocation(program, "aVertexPosition")
        aVertexNormal = GLES20.glGetAttribLocation(program, "aVertexNormal")
        uPMatrix = GLES20.glGetUniformLocation(program, "uPMatrix")
        uMVMatrix = GLES20.glGetUniformLocation(program, "uMVMatrix")
        uNMatrix = GLES20.glGetUniformLocation(program, "uNMatrix")
        uMaterialAmbient = GLES20.glGetUniformLocation(program, "uMaterialAmbient")
        uMaterialDiffuse = GLES20.glGetUniformLocation(program, "uMaterialDiffuse")
        uMaterialSpecular = GLES20.glGetUniformLocation(program, "uMaterialSpecular")
        uShininess = GLES20.glGetUniformLocation(program, "uShininess")
        uLightAmbient = GLES20.glGetUniformLocation(program, "uLightAmbient")
        uLightDiffuse = GLES20.glGetUniformLocation(program, "uLightDiffuse")
        uLightSpecular = GLES20.glGetUniformLocation(program, "uLightSpecular")
        uLightPosition = GLES20.glGetUniformLocation(program, "uLightPosition")

        GLES20.glUniform3fv(uLightPosition, 1, floatArrayOf(4.5f, -3.0f, 15.0f), 0)
        GLES20.glUniform4fv(uLightAmbient, 1, floatArrayOf(0.03f, 0.03f, 0.03f, 1.0f), 0)
        GLES20.glUniform4fv(uLightDiffuse, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)
        GLES20.glUniform4fv(uLightSpecular, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)

        GLES20.glUniform4fv(uMaterialAmbient, 1, floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f), 0)
        GLES20.glUniform4fv(uMaterialDiffuse, 1, floatArrayOf(0.5f, 0.8f, 0.1f, 1.0f), 0)
        GLES20.glUniform4fv(uMaterialSpecular, 1, floatArrayOf(0.6f, 0.6f, 0.6f, 1.0f), 0)

        GLES20.glUniform1f(uShininess, 200.0f)
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}