package com.zmd.lab.gles20.sample.ambient.goraud.phong

import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.zmd.lib.gles20.obj.ObjData
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.tan

class AmGLRenderer: GLSurfaceView.Renderer {
    val TAG = "AmGLRenderer"
    var sphere = ObjData("sphere")
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

                "uniform float uShininess;" +		 //shininness
                "uniform vec3 uLightDirection;" +	 //light direction

                "uniform vec4 uLightAmbient;" +      //light ambient property
                "uniform vec4 uLightDiffuse;" +      //light diffuse property
                "uniform vec4 uLightSpecular;" +     //light specular property

                "uniform vec4 uMaterialAmbient;" +	 //object ambient property
                "uniform vec4 uMaterialDiffuse;" +   //object diffuse property
                "uniform vec4 uMaterialSpecular;" +  //object specular property

                "varying vec4 vFinalColor;" +

                "void main(void) {" +

                    //Transformed vertex position
                    "vec4 vertex = uMVMatrix * vec4(aVertexPosition, 1.0);" +

                    //Transformed normal position
                    "vec3 N = vec3(uNMatrix * vec4(aVertexNormal, 1.0));" +

                    //Invert and normalize light to calculate lambertTerm
                    "vec3 L = normalize(uLightDirection);" +

                    //Lambert's cosine law
                    "float lambertTerm = clamp(dot(N,-L),0.0,1.0);" +

                    //Ambient Term
                    "vec4 Ia = uLightAmbient * uMaterialAmbient;" +

                    //Diffuse Term
                    "vec4 Id = vec4(0.0,0.0,0.0,1.0);" +

                    //Specular Term
                    "vec4 Is = vec4(0.0,0.0,0.0,1.0);" +

                    "Id = uLightDiffuse * uMaterialDiffuse * lambertTerm;" + //add diffuse term

                    "vec3 eyeVec = -vec3(vertex.xyz);" +
                    "vec3 E = normalize(eyeVec);" +
                    "vec3 R = reflect(L, N);" +
                    "float specular = pow(max(dot(R, E), 0.0), uShininess );" +

                    "Is = uLightSpecular * uMaterialSpecular * specular;" +	//add specular term

                    //Final color
                    "vFinalColor = Ia + Id + Is;" +
                    "vFinalColor.a = 1.0;" +

                    //Transformed vertex position
                    "gl_Position = uPMatrix * vertex;" +
                "}"
    private val fragmentShaderCode =
                "precision highp float;" +

                "varying vec4 vFinalColor;" +

                "void main(void) {" +
                "    gl_FragColor = vFinalColor;" +
                    //  "    gl_FragColor = vec4(0.6, 0.7, 0.2, 0.9);" +
                "}"

    private val mvMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val nMatrix = FloatArray(16)

    private var angle = 0.0f
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var normalBuffer: FloatBuffer? = null
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
    private var uLightDirection = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initProgram()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        //float ratio = (float) width / height;
        //float size = 1f;
        //float left = -ratio * size;
        //float right = ratio * size;
        //float bottom = -1 * size;
        //float top = 1 * size;
        //float near = 0.1f;
        //float far = 10000.0f;
        //Matrix.frustumM(pMatrix, 0, left, right, bottom, top, near, far);

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

        //perspective(45f, (float)width/height, 0.1f, 10000.0f, pMatrix);
        //Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
    }

    override fun onDrawFrame(gl: GL10?) {
        angle += 1f

        glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glClearDepthf(100.0f)
        glEnable(GL_DEPTH_TEST)
        glDepthFunc(GL_LEQUAL)

        glUseProgram(program)

        //Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, 0.0f, 0.0f, -2.5f)
        Matrix.rotateM(mvMatrix, 0, angle, 1.0f, 1.0f, 0.0f)

        glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)

        //copyMatrix(nMatrix, mvMatrix);
        //inverse(nMatrix, nMatrix);
        //transpose(nMatrix, nMatrix);

        Matrix.transposeM(nMatrix, 0, mvMatrix, 0)
        Matrix.invertM(nMatrix, 0, nMatrix, 0)

        glUniformMatrix4fv(uNMatrix, 1, false, nMatrix, 0)

        glEnableVertexAttribArray(aVertexPosition)
        glEnableVertexAttribArray(aVertexNormal)

        glVertexAttribPointer(aVertexPosition, COORDS_PER_VERTEX,
            GL_FLOAT, false,
            vertexStride, vertexBuffer)

        glVertexAttribPointer(aVertexNormal, COORDS_PER_VERTEX,
            GL_FLOAT, false,
            vertexStride, normalBuffer)

        glDrawElements(GL_TRIANGLES, sphere.indices.size,
            GL_UNSIGNED_SHORT, indexBuffer)

        glDisableVertexAttribArray(aVertexPosition)
        glDisableVertexAttribArray(aVertexNormal)
    }

    private fun initBuffer() {
        // number of coordinate values * 4 bytes per float
        val vb = ByteBuffer.allocateDirect(sphere.vertices.size * 4)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer!!.put(sphere.vertices)
        vertexBuffer!!.position(0)

        // # of coordinate values * 2 bytes per short
        val ib = ByteBuffer.allocateDirect(sphere.indices.size * 2)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer!!.put(sphere.indices)
        indexBuffer!!.position(0)

        val nb = ByteBuffer.allocateDirect(sphere.normals.size * 4)
        nb.order(ByteOrder.nativeOrder())
        normalBuffer = nb.asFloatBuffer()
        normalBuffer!!.put(sphere.normals)
        normalBuffer!!.position(0)
    }

    private fun initProgram() {
        val vertexShader = loadShader(GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = glCreateProgram()
        glAttachShader(program, vertexShader)
        glAttachShader(program, fragmentShader)
        glLinkProgram(program)

        glUseProgram(program)

        aVertexPosition = glGetAttribLocation(program, "aVertexPosition")
        aVertexNormal = glGetAttribLocation(program, "aVertexNormal")
        uPMatrix = glGetUniformLocation(program, "uPMatrix")
        uMVMatrix = glGetUniformLocation(program, "uMVMatrix")
        uNMatrix = glGetUniformLocation(program, "uNMatrix")
        uMaterialAmbient = glGetUniformLocation(program, "uMaterialAmbient")
        uMaterialDiffuse = glGetUniformLocation(program, "uMaterialDiffuse")
        uMaterialSpecular = glGetUniformLocation(program, "uMaterialSpecular")
        uShininess = glGetUniformLocation(program, "uShininess")
        uLightAmbient = glGetUniformLocation(program, "uLightAmbient")
        uLightDiffuse = glGetUniformLocation(program, "uLightDiffuse")
        uLightSpecular = glGetUniformLocation(program, "uLightSpecular")
        uLightDirection = glGetUniformLocation(program, "uLightDirection")

        glUniform3fv(uLightDirection, 1, floatArrayOf(-0.25f, -0.25f, -0.25f), 0);
        glUniform4fv(uLightAmbient, 1, floatArrayOf(0.03f, 0.03f, 0.03f, 1.0f), 0);
        glUniform4fv(uLightDiffuse, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0);
        glUniform4fv(uLightSpecular, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0);

        glUniform4fv(uMaterialAmbient, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0);
        glUniform4fv(uMaterialDiffuse, 1, floatArrayOf(46f / 256f, 99f / 256f, 191f/256f, 1.0f), 0);
        glUniform4fv(uMaterialSpecular, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0);

        glUniform1f(uShininess, 10.0f);
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, shaderCode)
        glCompileShader(shader)
        return shader
    }

    fun checkGlError(glOperation: String) {
        var error = 0
        while(glGetError().also { error = it } != GL_NO_ERROR) {
            Log.e(TAG, "$glOperation: glError $error")
            throw RuntimeException("$glOperation: glError $error")
        }
    }

    private fun copyMatrix(result: FloatArray, origin: FloatArray) {
        for (i in origin.indices) {
            result[i] = origin[i]
        }
    }
}