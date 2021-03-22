package com.zmd.lab.gles20.sample.toon

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

class ToonGLRenderer: GLSurfaceView.Renderer {
    private val TAG = "ToonGLRenderer"

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

        "varying vec3 vNormal;" +
        "varying vec3 vVertex;" +

        "void main(void) {" +

            "vec4 normal = uNMatrix * vec4(aVertexNormal, 1.0);" +
            "vNormal = normal.xyz;" +
            "vVertex = aVertexPosition;" +

            "gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);" +
        "}"
    private val fragmentShaderCode =
        "precision highp float;" +

        "uniform float uShininess;" +
        "uniform mat4 uMVMatrix;" +
        "uniform vec3 uLightDirection;" +
        "uniform vec3 uLightAmbient;" +
        "uniform vec3 uLightDiffuse;" +
        "uniform vec3 uMaterialDiffuse;" +

        "varying vec3 vNormal;" +
        "varying vec3 vVertex;" +

        "void main(void)  {" +

            "vec4 color0 = vec4(uMaterialDiffuse, 1.0);" + // Material Color
            "vec4 color1 = vec4(0.0, 0.0, 0.0, 1.0);" +    // Silhouette Color
            "vec4 color2 = vec4(uMaterialDiffuse, 1.0);" + // Specular Color

            "vec3 N = vNormal;" +
            "vec3 L = normalize(uLightDirection);" +

            "vec4 eyePos = uMVMatrix * vec4(0.0,0.0,0.0,1.0);" + //Extract the location of the camera

            "vec3 EyeVert = normalize(-eyePos.xyz);" + // invert to obtain eye position

            "vec3 EyeLight = normalize(L+EyeVert);" +

            // Simple Silhouette
            "float sil = max(dot(N,EyeVert), 0.0);" +
            "if (sil < 0.4) {" +
                "gl_FragColor = color1;" +
            "}" +
            "else" +
            "{" +
                "gl_FragColor = color0;" +

                // Specular part
                "float spec = pow(max(dot(N,EyeLight),0.0), uShininess);" +

                "if (spec < 0.2) gl_FragColor *= 0.8;" +
                "else gl_FragColor = color2;" +

                // Diffuse part
                "float diffuse = max(dot(N,L),0.0);" +
                "if (diffuse < 0.5) gl_FragColor *=0.8;" +
            "}" +
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
    private var uShininess = 0
    private var uLightAmbient = 0
    private var uLightDiffuse = 0
    private var uMaterialDiffuse = 0
    private var uLightDirection = 0

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
        //angle += 0.1f

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(100.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, 0.0f, 0.0f, -3.5f)
        Matrix.rotateM(mvMatrix, 0, angle, 0.0f, 0.0f, 1.0f)

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
            GLES20.GL_UNSIGNED_SHORT, indexBuffer)
//        GLES20.glDrawElements(GLES20.GL_LINES, sphere.getIndices().length,
//                GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aVertexPosition);
        GLES20.glDisableVertexAttribArray(aVertexNormal);
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
        uShininess = GLES20.glGetUniformLocation(program, "uShininess")
        uLightAmbient = GLES20.glGetUniformLocation(program, "uLightAmbient")
        uLightDiffuse = GLES20.glGetUniformLocation(program, "uLightDiffuse")
        uMaterialDiffuse = GLES20.glGetUniformLocation(program, "uMaterialDiffuse")
        uLightDirection = GLES20.glGetUniformLocation(program, "uLightDirection")

        GLES20.glUniform3fv(uLightDirection, 1, floatArrayOf(0.0f, 1.0f, 1.0f), 0)
        GLES20.glUniform3fv(uLightAmbient, 1, floatArrayOf(0.01f, 0.01f, 0.01f), 0)
        GLES20.glUniform3fv(uLightDiffuse, 1, floatArrayOf(0.5f, 0.5f, 0.5f), 0)
        GLES20.glUniform3fv(uMaterialDiffuse, 1, floatArrayOf(0.5f, 0.8f, 0.1f), 0)

        GLES20.glUniform1f(uShininess, 20.0f)
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}