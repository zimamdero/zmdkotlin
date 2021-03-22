package com.zmd.lab.gles20.sample.cubemap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.*
import android.util.Log
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

class CMRenderer(val context: Context): GLSurfaceView.Renderer {
    val TAG = "CMRenderer"

    var obj = ObjData("cubemap")
        set(value) {
            field = value
            initBuffer()
        }
    private var camera = GLCamera(GLCamera.CAMERA_TYPE_TRACKING)

    private val vertexShaderCode =
    //geometry
    "attribute vec3 aVertexPosition;" +
    "attribute vec3 aVertexNormal;" +
    "attribute vec2 aVertexTextureCoords;" +

    //matrices
    "uniform mat4 uMVMatrix;" +
    "uniform mat4 uPMatrix;" +
    "uniform mat4 uNMatrix;" +

    //varyings
    "varying vec2 vTextureCoord;" +
    "varying vec3 vVertexNormal;" +

    "void main(void) {" +
        //Final vertex position
        "gl_Position = uPMatrix * uMVMatrix * vec4(aVertexPosition, 1.0);" +
        "vTextureCoord = aVertexTextureCoords;" +
        "vVertexNormal = (uNMatrix * vec4(-aVertexPosition, 1.0)).xyz;" +
    "}";
    private val fragmentShaderCode =
    "precision highp float;" +
    //sampler
    "uniform sampler2D uSampler;" +
    "uniform samplerCube uCubeSampler;" +

    //varying
    "varying vec2 vTextureCoord;" +
    "varying vec3 vVertexNormal;" +

    "void main(void)" +
    "{" +
    "    gl_FragColor = texture2D(uSampler, vTextureCoord) * textureCube(uCubeSampler, vVertexNormal);" +
    //        "    gl_FragColor = textureCube(uCubeSampler, vVertexNormal);" +
    //"    gl_FragColor = texture2D(uSampler, vTextureCoord);" +
    "}";

    private val mvMatrix = FloatArray(16)
    private val pMatrix = FloatArray(16)
    private val nMatrix = FloatArray(16)

    private var angle = 0.0f
    private var cameraElecation = 0.0f
    private var cameraAzimuth = 0.0f

    private var vertexBuffer: FloatBuffer?  = null
    private var normalsBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null

    private var program = 0

    private val COORDS_PER_VERTEX = 3
    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private var aVertexPosition = 0
    private var aVertexNormal = 0
    private var aVertexTextureCoords = 0
    private var uPMatrix = 0
    private var uMVMatrix = 0
    private var uNMatrix = 0
    private var uSampler = 0
    private var uCubeSampler = 0

    private var texNames = IntArray(1)
    private var cubeTexture = IntArray(1)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        initProgram()

        //camera = new GLCamera(GLCamera.CAMERA_TYPE_TRACKING);
        camera = GLCamera(GLCamera.CAMERA_TYPE_ORBIT)
        camera.goHome(floatArrayOf(0f, 0f, 3.5f, 0f))
        //camera.setElevation(45f);

//        AssetManager assetManager = context.getAssets();
//        InputStream input;
//        Bitmap bitmap = null;
//        try {
//            input = assetManager.open("webgl.png");
//            bitmap = BitmapFactory.decodeStream(input);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        val bitmap = getBitmap("img00.jpg")

        GLES20.glEnable(GLES20.GL_TEXTURE_2D)
        texNames = IntArray(1)
        GLES20.glGenTextures(1, texNames, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_NEAREST);
        //GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        val positiveXBitmap = getBitmap("posx.jpg")
        val negativeXBitmap = getBitmap("negx.jpg")
        val positiveYBitmap = getBitmap("posy.jpg")
        val negativeYBitmap = getBitmap("negy.jpg")
        val positiveZBitmap = getBitmap("posz.jpg")
        val negativeZBitmap = getBitmap("negz.jpg")

        cubeTexture = IntArray(1)
        GLES20.glGenTextures(1, cubeTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, cubeTexture[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, positiveXBitmap, 0)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, negativeXBitmap, 0)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, positiveYBitmap, 0)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, negativeYBitmap, 0)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, positiveZBitmap, 0)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, negativeZBitmap, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height);

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
        //angle = 180f
        cameraAzimuth += 0.1f
        cameraElecation += 0.1f
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
        Matrix.rotateM(mvMatrix, 0, angle, 0.0f, 0.0f, 1.0f)

        Matrix.multiplyMM(mvMatrix, 0, camera.getViewM(), 0, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uMVMatrix, 1, false, mvMatrix, 0)
        GLES20.glUniformMatrix4fv(uPMatrix, 1, false, pMatrix, 0)

        Matrix.transposeM(nMatrix, 0, camera.getViewM(), 0)
        GLES20.glUniformMatrix4fv(uNMatrix, 1, false, nMatrix, 0)

        GLES20.glEnableVertexAttribArray(aVertexPosition)

        GLES20.glVertexAttribPointer(aVertexPosition, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer)

        GLES20.glEnableVertexAttribArray(aVertexTextureCoords)
        GLES20.glVertexAttribPointer(aVertexTextureCoords, 2, GLES20.GL_FLOAT, false, 2 * 4, texCoordBuffer)
        //GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texNames[0]);
        //GLES20.glUniform1i(uSampler, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, cubeTexture[0])
        GLES20.glUniform1i(uCubeSampler, 1)

        //GLES20.glDrawElements(GLES20.GL_LINE_LOOP, obj.getIndices().length,
        //        GLES20.GL_UNSIGNED_SHORT, indexBuffer);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, obj.indices.size,
                GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        //GLES20.glDrawElements(GLES20.GL_LINES, sphere.getIndices().length,
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
        aVertexTextureCoords = GLES20.glGetAttribLocation(program, "aVertexTextureCoords")
        uPMatrix = GLES20.glGetUniformLocation(program, "uPMatrix")
        uMVMatrix = GLES20.glGetUniformLocation(program, "uMVMatrix")
        uNMatrix = GLES20.glGetUniformLocation(program, "uNMatrix")
        uSampler = GLES20.glGetUniformLocation(program, "uSampler")
        uCubeSampler = GLES20.glGetUniformLocation(program, "uCubeSampler")
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun checkGlError(glOperation: String) {
        var error = 0
        while ((GLES20.glGetError().also { error = it }) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$glOperation: glError $error")
            throw RuntimeException("$glOperation: glError $error")
        }
    }

    private fun getBitmap(imgFile: String): Bitmap? {
        val assetManager = context.assets
        var bitmap: Bitmap? = null
        try {
            val input = assetManager.open(imgFile)
            bitmap = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace();
        }

        return bitmap
    }
}