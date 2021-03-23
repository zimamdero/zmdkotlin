package com.zmd.lib.gles20.sample.normalmap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import com.zmd.lib.gles20.camera.GLCamera
import com.zmd.lib.gles20.obj.ObjData
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.tan

class NMRenderer(val context: Context): GLSurfaceView.Renderer {
    private val TAG = "NMRenderer"

    var obj = NormalObjData("cube")
        set(value) {
            field = value
            initBuffer()
        }
    private var camera = GLCamera(GLCamera.CAMERA_TYPE_TRACKING)

    private val vertexShaderCode =
            "attribute vec3 aVertexPosition;" +
            "attribute vec3 aVertexNormal;" +
            "attribute vec3 aVertexTangent;" +
            "attribute vec2 aVertexTextureCoords;" +

            //matrices
            "uniform mat4 uMVMatrix;" +
            "uniform mat4 uPMatrix;" +
            "uniform mat4 uNMatrix;" +

            //lights
            "uniform vec3 uLightPosition;" +

            //varyings
            "varying vec2 vTextureCoord;" +
            "varying vec3 vTangentLightDir;" +
            "varying vec3 vTangentEyeDir;" +

            "void main(void) {" +
                //Transformed vertex position
                "vec4 vertex = uMVMatrix * vec4(aVertexPosition, 1.0);" +

                //Transformed normal position
                "vec3 normal = vec3(uNMatrix * vec4(aVertexNormal, 1.0));" +
                "vec3 tangent = vec3(uNMatrix * vec4(aVertexTangent, 1.0));" +
                "vec3 bitangent = cross(normal, tangent);" +

                "mat3 tbnMatrix = mat3(" +
                    "tangent.x, bitangent.x, normal.x," +
                    "tangent.y, bitangent.y, normal.y," +
                    "tangent.z, bitangent.z, normal.z" +
                ");" +

                //light direction, from light position to vertex
                "vec3 lightDirection = uLightPosition - vertex.xyz;" +

                //eye direction, from camera position to vertex
                "vec3 eyeDirection = -vertex.xyz;" +

                //Final vertex position
                "gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);" +
                "vTextureCoord = aVertexTextureCoords;" +
                "vTangentLightDir = lightDirection * tbnMatrix;" +
                "vTangentEyeDir = eyeDirection * tbnMatrix;" +
            "}"
    private val fragmentShaderCode =
            "precision highp float;" +
            "uniform vec4 uMaterialDiffuse;" +
            "uniform vec4 uMaterialAmbient;" +

            "uniform vec4 uLightAmbient;" +
            "uniform vec4 uLightDiffuse;" +

            //samplers
            "uniform sampler2D uSampler;" +
            "uniform sampler2D uNormalSampler;" +

            //varying
            "varying vec2 vTextureCoord;" +
            "varying vec3 vTangentLightDir;" +
            "varying vec3 vTangentEyeDir;" +

            "void main(void)" +
            "{" +
                // Unpack tangent-space normal from texture
                "vec3 normal = normalize(2.0 * (texture2D(uNormalSampler, vTextureCoord).rgb - 0.5));" +

                // Normalize the light direction and determine how much light is hitting this point
                "vec3 lightDirection = normalize(vTangentLightDir);" +
                "float lambertTerm = max(dot(normal,lightDirection),0.20);" +

                // Calculate Specular level
                "vec3 eyeDirection = normalize(vTangentEyeDir);" +
                "vec3 reflectDir = reflect(-lightDirection, normal);" +
                "float Is = pow(clamp(dot(reflectDir, eyeDirection), 0.0, 1.0), 8.0);" +

                // Combine lighting and material colors
                "vec4 Ia = uLightAmbient * uMaterialAmbient;" +
                "vec4 Id = uLightDiffuse * uMaterialDiffuse * texture2D(uSampler, vTextureCoord) * lambertTerm;" +

                "gl_FragColor = Ia + Id + Is;" +

                ////"gl_FragColor = texture2D(uNormalSampler, vTextureCoord);" +
                ////"gl_FragColor = texture2D(uSampler, vTextureCoord);" +
            "}"

    private val mvMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val nMatrix = FloatArray(16)

    private var angle = 0.0f
    private var cameraElecation = 0.0f
    private var cameraAzimuth = 0.0f

    private var vertexBuffer: FloatBuffer? = null
    private var normalsBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null
    private var tangentBuffer: FloatBuffer? = null

    private var program = 0

    private val COORDS_PER_VERTEX = 3
    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private var aVertexPosition = 0
    private var aVertexNormal = 0
    private var aVertexTangent = 0
    private var aVertexTextureCoords = 0
    private var uMaterialDiffuse = 0
    private var uMaterialAmbient = 0
    private var uMVMatrix = 0
    private var uPMatrix = 0
    private var uNMatrix = 0
    private var uLightPosition = 0
    private var uLightAmbient = 0
    private var uLightDiffuse = 0
    private var uSampler = 0
    private var uNormalSampler = 0

    private var texNames = IntArray(1)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        initProgram()

        //camera = new GLCamera(GLCamera.CAMERA_TYPE_TRACKING);
        camera = GLCamera(GLCamera.CAMERA_TYPE_ORBIT)
        camera.goHome(floatArrayOf(0f, 0f, 5f, 0f))
        camera.setAzimuth(45f)
        camera.setElevation(-30f)

        val bitmap = getBitmap("stonewall.jpeg")
        val bitmap2 = getBitmap("stonewall-normal.png")
        //val bitmap = getBitmap("fieldstone.jpg")
        //val bitmap2 = getBitmap("fieldstone-normal.jpg")

        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        texNames = IntArray(2)
        GLES20.glGenTextures(2, texNames, 0)
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

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[1])
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap2, 0)
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
        angle += 0.5f;
        //cameraAzimuth += 0.1f
        //cameraElecation += 0.51f
        camera.setElevation(cameraElecation)
        camera.setAzimuth(cameraAzimuth)

        GLES20.glClearColor(0.3f, 0.3f, 0.3f, 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(100.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        GLES20.glUseProgram(program)

        Matrix.setIdentityM(mvMatrix, 0)
        Matrix.translateM(mvMatrix, 0, 0.0f, 0.0f, 0.0f)
        Matrix.rotateM(mvMatrix, 0, angle, 1.0f, 1.0f, 0.0f)

        Matrix.multiplyMM(mvMatrix, 0, camera.getViewM(), 0, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)

        Matrix.setIdentityM(nMatrix, 0)
        Matrix.transposeM(nMatrix, 0, mvMatrix, 0)
        Matrix.invertM(nMatrix, 0, nMatrix, 0)
        GLES20.glUniformMatrix4fv(uNMatrix, 1, false, nMatrix, 0)

        GLES20.glEnableVertexAttribArray(aVertexPosition)

        GLES20.glVertexAttribPointer(aVertexPosition, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aVertexNormal)
        GLES20.glVertexAttribPointer(aVertexNormal, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false,
            vertexStride, normalsBuffer)

        GLES20.glEnableVertexAttribArray(aVertexTextureCoords)
        GLES20.glVertexAttribPointer(aVertexTextureCoords, 2, GLES20.GL_FLOAT, false, 2 * 4, texCoordBuffer)

        GLES20.glEnableVertexAttribArray(aVertexTangent)
        GLES20.glVertexAttribPointer(aVertexTangent, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, tangentBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0])
        GLES20.glUniform1i(uSampler, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[1])
        GLES20.glUniform1i(uNormalSampler, 1)

        //GLES20.glDrawElements(GLES20.GL_LINE_LOOP, obj.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, obj.indices.size,
            GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        //GLES20.glDrawElements(GLES20.GL_LINES, obj.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);

        GLES20.glDisableVertexAttribArray(aVertexPosition)
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

        val txb = ByteBuffer.allocateDirect(obj.textureCodes.size * 4)
        txb.order(ByteOrder.nativeOrder())
        texCoordBuffer = txb.asFloatBuffer()
        texCoordBuffer?.put(obj.textureCodes)
        texCoordBuffer?.position(0)

        val tgb = ByteBuffer.allocateDirect(obj.tangents.size * 4)
        tgb.order(ByteOrder.nativeOrder())
        tangentBuffer = tgb.asFloatBuffer()
        tangentBuffer?.put(obj.tangents)
        tangentBuffer?.position(0)
    }

    private fun initProgram() {
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        aVertexPosition = GLES20.glGetAttribLocation(program, "aVertexPosition")
        aVertexNormal = GLES20.glGetAttribLocation(program, "aVertexNormal")
        aVertexTangent = GLES20.glGetAttribLocation(program, "aVertexTangent")
        aVertexTextureCoords = GLES20.glGetAttribLocation(program, "aVertexTextureCoords")
        uMaterialDiffuse = GLES20.glGetUniformLocation(program, "uMaterialDiffuse")
        uMaterialAmbient = GLES20.glGetUniformLocation(program, "uMaterialAmbient")
        uPMatrix = GLES20.glGetUniformLocation(program, "uPMatrix")
        uMVMatrix = GLES20.glGetUniformLocation(program, "uMVMatrix")
        uNMatrix = GLES20.glGetUniformLocation(program, "uNMatrix")
        uLightPosition = GLES20.glGetUniformLocation(program, "uLightPosition")
        uLightAmbient = GLES20.glGetUniformLocation(program, "uLightAmbient")
        uLightDiffuse = GLES20.glGetUniformLocation(program, "uLightDiffuse")
        uSampler = GLES20.glGetUniformLocation(program, "uSampler")
        uNormalSampler = GLES20.glGetUniformLocation(program, "uNormalSampler")

        GLES20.glUseProgram(program)
        GLES20.glUniform3fv(uLightPosition, 1, floatArrayOf(1f, 10f, 1f), 0)
        GLES20.glUniform4fv(uLightAmbient, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)
        GLES20.glUniform4fv(uLightDiffuse, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)

        GLES20.glUniform4fv(uMaterialAmbient, 1, floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f), 0)
        GLES20.glUniform4fv(uMaterialDiffuse, 1, floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f), 0)
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    private fun getBitmap(imgPath: String): Bitmap? {
        val assetManager = context.assets
        var bitmap: Bitmap? = null
        try {
            val input = assetManager.open(imgPath)
            bitmap = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bitmap
    }
}