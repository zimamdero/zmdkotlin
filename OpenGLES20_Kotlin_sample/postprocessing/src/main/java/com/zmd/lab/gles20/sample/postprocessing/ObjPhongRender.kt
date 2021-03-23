package com.zmd.lab.gles20.sample.postprocessing

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.zmd.lib.gles20.camera.GLCamera
import com.zmd.lib.gles20.obj.ObjData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class ObjPhongRender(context: Context, name: String, path: String) {
    private val TAG = "ObjPhongRender"

    var glDrawType = GLES20.GL_TRIANGLES

    var obj = ObjData()
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
            "varying vec3 vEyeVec;" +
            "void main(void) {" +  //Transformed vertex position
                "vec4 vertex = uMVMatrix * vec4(aVertexPosition, 1.0);" +  //Transformed normal position
                "vNormal = vec3(uNMatrix * vec4(aVertexNormal, 1.0));" +  //Vector Eye
                "vEyeVec = -vec3(vertex.xyz);" +  // camera??
                //Final vertex position
                "gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);" +
            "}"
    private val fragmentShaderCode =
            "precision highp float;" +
            "uniform float uShininess;" +  //shininess
            "uniform vec3 uLightDirection;" +  //light direction
            "uniform vec4 uLightAmbient;" +  //light ambient property
            "uniform vec4 uLightDiffuse;" +  //light diffuse property
            "uniform vec4 uLightSpecular;" +  //light specular property
            "uniform vec4 uMaterialAmbient;" +  //object ambient property
            "uniform vec4 uMaterialDiffuse;" +  //object diffuse property
            "uniform vec4 uMaterialSpecular;" +  //object specular property
            "varying vec3 vNormal;" +
            "varying vec3 vEyeVec;" +
            "void main(void)" +
            "{" +
                "vec3 L = normalize(uLightDirection);" +
                "vec3 N = normalize(vNormal);" +  //Lambert's cosine law
                "float lambertTerm = dot(N,-L);" +  //Ambient Term
                "vec4 Ia = uLightAmbient * uMaterialAmbient;" +  //Diffuse Term
                "vec4 Id = vec4(0.0,0.0,0.0,1.0);" +  //Specular Term
                "vec4 Is = vec4(0.0,0.0,0.0,1.0);" +
                "if(lambertTerm > 0.0)" +  //only if lambertTerm is positive
                "{" +
                    "Id = uLightDiffuse * uMaterialDiffuse * lambertTerm;" +  //add diffuse term
                    "vec3 E = normalize(vEyeVec);" +
                    "vec3 R = reflect(L, N);" +
                    "float specular = pow( max(dot(R, E), 0.0), uShininess);" +
                    "Is = uLightSpecular * uMaterialSpecular * specular;" +  //add specular term
                "}" +  //Final color
                "vec4 finalColor = Ia + Id + Is;" +
                "finalColor.a = 1.0;" +
                "gl_FragColor = finalColor;" +
            "}"

    private var posX = 0.0f
    private var posY = 0.0f
    private var posZ = 0.0f
    private var rotX = 0.0f
    private var rotY = 0.0f
    private var rotZ = 0.0f
    var scaleX = 1.0f
    var scaleY = 1.0f
    var scaleZ = 1.0f

    private val mvMatrix = FloatArray(16)
    private val nMatrix = FloatArray(16)

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
    private var uLightDirection = 0

    private val lightDirection = floatArrayOf(0.0f, -1.0f, -1.0f)
    private val lightAmbient = floatArrayOf(0.03f, 0.03f, 0.03f, 1.0f)
    private val lightDiffuse = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private val lightSpecular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    private val materialAmbient = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private val materialDiffuse = floatArrayOf(0.1f, 0.8f, 0.8f, 1.0f)
    private val materialSpecular = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

    private var shininess = 230.0f

    init {
        val objData = ObjData(name)
        objData.load(context, path)
        obj = objData
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

    fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
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
        uLightDirection = GLES20.glGetUniformLocation(program, "uLightDirection")
        GLES20.glUseProgram(program)
        GLES20.glUniform3fv(uLightDirection, 1, lightDirection, 0)
        GLES20.glUniform4fv(uLightAmbient, 1, lightAmbient, 0)
        GLES20.glUniform4fv(uLightDiffuse, 1, lightDiffuse, 0)
        GLES20.glUniform4fv(uLightSpecular, 1, lightSpecular, 0)
        GLES20.glUniform4fv(uMaterialAmbient, 1, materialAmbient, 0)
        GLES20.glUniform4fv(uMaterialDiffuse, 1, materialDiffuse, 0)
        GLES20.glUniform4fv(uMaterialSpecular, 1, materialSpecular, 0)
        GLES20.glUniform1f(uShininess, shininess)
    }

    fun draw(pMatrix: FloatArray?, camera: GLCamera) {
        GLES20.glUseProgram(program)
        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, posX, posY, posZ)
        Matrix.scaleM(mvMatrix, 0, scaleX, scaleY, scaleZ)
        Matrix.rotateM(mvMatrix, 0, rotX, 1.0f, 0.0f, 0.0f)
        Matrix.rotateM(mvMatrix, 0, rotY, 0.0f, 1.0f, 0.0f)
        Matrix.rotateM(mvMatrix, 0, rotZ, 0.0f, 0.0f, 1.0f)
        Matrix.multiplyMM(mvMatrix, 0, camera.getViewM(), 0, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)
        Matrix.transposeM(nMatrix, 0, camera.getViewM(), 0)
        //Matrix.transposeM(nMatrix, 0, mvMatrix, 0);
        //Matrix.invertM(nMatrix, 0, nMatrix, 0);
        GLES20.glUniformMatrix4fv(uNMatrix, 1, false, nMatrix, 0)
        GLES20.glEnableVertexAttribArray(aVertexPosition)
        GLES20.glEnableVertexAttribArray(aVertexNormal)
        GLES20.glVertexAttribPointer(
            aVertexPosition, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer
        )
        GLES20.glVertexAttribPointer(
            aVertexNormal, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, normalsBuffer
        )
        GLES20.glDrawElements(
            glDrawType, obj.indices.size,
            GLES20.GL_UNSIGNED_SHORT, indexBuffer
        )
        //GLES20.glDrawElements(GLES20.GL_LINES, sphere.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDisableVertexAttribArray(aVertexPosition)
        GLES20.glDisableVertexAttribArray(aVertexNormal)
    }

    fun loadShader(type: Int, shaderCode: String?): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun place(x: Float, y: Float, z: Float) {
        posX = x
        posY = y
        posZ = z
    }

    fun rotate(x: Float, y: Float, z: Float) {
        rotX = x
        rotY = y
        rotZ = z
    }

    fun setScale(x: Float, y: Float, z: Float) {
        scaleX = x
        scaleY = y
        scaleZ = z
    }

    fun setLightDirection(x: Float, y: Float, z: Float) {
        lightDirection[0] = x
        lightDirection[1] = y
        lightDirection[2] = z
    }

    fun setLightAmbient(r: Float, g: Float, b: Float, a: Float) {
        lightAmbient[0] = r
        lightAmbient[1] = g
        lightAmbient[2] = b
        lightAmbient[3] = a
    }

    fun setLightDiffuse(r: Float, g: Float, b: Float, a: Float) {
        lightDiffuse[0] = r
        lightDiffuse[1] = g
        lightDiffuse[2] = b
        lightDiffuse[3] = a
    }

    fun setLightSpecular(r: Float, g: Float, b: Float, a: Float) {
        lightSpecular[0] = r
        lightSpecular[1] = g
        lightSpecular[2] = b
        lightSpecular[3] = a
    }

    fun setMaterialAmbient(r: Float, g: Float, b: Float, a: Float) {
        materialAmbient[0] = r
        materialAmbient[1] = g
        materialAmbient[2] = b
        materialAmbient[3] = a
    }

    fun setMaterialDiffuse(r: Float, g: Float, b: Float, a: Float) {
        materialDiffuse[0] = r
        materialDiffuse[1] = g
        materialDiffuse[2] = b
        materialDiffuse[3] = a
    }

    fun setMaterialSpecular(r: Float, g: Float, b: Float, a: Float) {
        materialSpecular[0] = r
        materialSpecular[1] = g
        materialSpecular[2] = b
        materialSpecular[3] = a
    }
}