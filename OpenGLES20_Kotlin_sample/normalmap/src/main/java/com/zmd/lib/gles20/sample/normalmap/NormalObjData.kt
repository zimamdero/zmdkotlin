package com.zmd.lib.gles20.sample.normalmap

import android.content.Context
import com.zmd.lib.gles20.obj.ObjData
import kotlin.math.sqrt

class NormalObjData(name: String) {
    var obj = ObjData(name)
    val vertices get() = obj.vertices
    val indices get() = obj.indices
    val diffuse get() = obj.diffuse
    val normals get() = obj.normals
    val textureCodes get() = obj.textureCodes
    val scalars get() = obj.scalars

    var tangents = FloatArray(1)

    fun load(context: Context, fileName: String) {
        obj.load(context, fileName)
        calculateTangents()
    }

    private fun calculateTangents() {
        val tangents = Array<FloatArray>(vertices.size/3) { FloatArray(3) }
        for(i in 0 until vertices.size/3){
            tangents[i] = floatArrayOf(0f, 0f, 0f)
        }

        // Calculate tangents
        for(i in indices.indices step 3) {
            val i0 = indices[i+0]
            val i1 = indices[i+1]
            val i2 = indices[i+2]

            val pos0 = floatArrayOf(vertices[i0 * 3], vertices[i0 * 3 + 1], vertices[i0 * 3 + 2])
            val pos1 = floatArrayOf(vertices[i1 * 3], vertices[i1 * 3 + 1], vertices[i1 * 3 + 2])
            val pos2 = floatArrayOf(vertices[i2 * 3], vertices[i2 * 3 + 1], vertices[i2 * 3 + 2])

            val tex0 = floatArrayOf(textureCodes[i0 * 2], textureCodes[i0 * 2 + 1])
            val tex1 = floatArrayOf(textureCodes[i1 * 2], textureCodes[i1 * 2 + 1])
            val tex2 = floatArrayOf(textureCodes[i2 * 2], textureCodes[i2 * 2 + 1])

            val a = floatArrayOf(0f, 0f, 0f)
            val b = floatArrayOf(0f, 0f, 0f)

            subtractVec3(pos1, pos0, a)
            subtractVec3(pos2, pos0, b)

            val c2c1t = tex1[0] - tex0[0]
            val c2c1b = tex1[1] - tex0[1]
            val c3c1t = tex2[0] - tex0[0]
            val c3c1b = tex2[0] - tex0[1]

            val triTangent = floatArrayOf(c3c1b * a[0] - c2c1b * b[0], c3c1b * a[1] - c2c1b * b[1], c3c1b * a[2] - c2c1b * b[2])

            addVec3(tangents[i0.toInt()], triTangent)
            addVec3(tangents[i1.toInt()], triTangent)
            addVec3(tangents[i2.toInt()], triTangent)
        }

        // Normalize tangents
//        this.tangents = FloatArray(tangents.size * 3)
//        for (i in tangents.indices) {
//            val index = i * 3
//            val tan = tangents[i]
//            normalizeVec3(tan)
//            this.tangents[index] = tan[0]
//            this.tangents[index + 1] = tan[1]
//            this.tangents[index + 2] = tan[2]
//        }
        val tlist = ArrayList<Float>()
        for (i in tangents.indices) {
            val tan = tangents[i]
            normalizeVec3(tan)
            tlist.add(tan[0])
            tlist.add(tan[1])
            tlist.add(tan[2])
        }
        this.tangents = tlist.toFloatArray()
    }

    private fun subtractVec3(a: FloatArray, b: FloatArray, result: FloatArray) {
        result[0] = a[0] - b[0]
        result[1] = a[1] - b[1]
        result[2] = a[2] - b[2]
    }

    private fun addVec3(result: FloatArray, b: FloatArray) {
        result[0] += b[0]
        result[1] += b[1]
        result[2] += b[2]
    }

    private fun normalizeVec3(result: FloatArray) {
        val c = result[0]
        val d = result[1]
        val e = result[2]
        var g = sqrt((c*c + d*d + e*e).toDouble()).toFloat()

        if (g == 0f) {
            result[0] = 0f
            result[1] = 0f
            result[2] = 0f
            return
        }
        if (1f == g) {
            return
        }

        g = 1/g
        result[0] = c * g
        result[1] = d * g
        result[2] = e * g
    }
}