package com.infinityrush.game3d

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class CubeMesh {
    private val vertices = floatArrayOf(
        -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
        0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
        0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
        -0.5f, -0.5f, 0.5f, 0f, 0f, 1f,
        0.5f, 0.5f, 0.5f, 0f, 0f, 1f,
        -0.5f, 0.5f, 0.5f, 0f, 0f, 1f,

        0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
        -0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
        -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
        0.5f, -0.5f, -0.5f, 0f, 0f, -1f,
        -0.5f, 0.5f, -0.5f, 0f, 0f, -1f,
        0.5f, 0.5f, -0.5f, 0f, 0f, -1f,

        -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
        -0.5f, -0.5f, 0.5f, -1f, 0f, 0f,
        -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
        -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,
        -0.5f, 0.5f, 0.5f, -1f, 0f, 0f,
        -0.5f, 0.5f, -0.5f, -1f, 0f, 0f,

        0.5f, -0.5f, 0.5f, 1f, 0f, 0f,
        0.5f, -0.5f, -0.5f, 1f, 0f, 0f,
        0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
        0.5f, -0.5f, 0.5f, 1f, 0f, 0f,
        0.5f, 0.5f, -0.5f, 1f, 0f, 0f,
        0.5f, 0.5f, 0.5f, 1f, 0f, 0f,

        -0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
        0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
        0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
        -0.5f, 0.5f, 0.5f, 0f, 1f, 0f,
        0.5f, 0.5f, -0.5f, 0f, 1f, 0f,
        -0.5f, 0.5f, -0.5f, 0f, 1f, 0f,

        -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
        0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
        0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
        -0.5f, -0.5f, -0.5f, 0f, -1f, 0f,
        0.5f, -0.5f, 0.5f, 0f, -1f, 0f,
        -0.5f, -0.5f, 0.5f, 0f, -1f, 0f
    )

    private val buffer: FloatBuffer = ByteBuffer
        .allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(vertices)
            position(0)
        }

    fun draw(positionHandle: Int, normalHandle: Int) {
        buffer.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glEnableVertexAttribArray(positionHandle)

        buffer.position(3)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glEnableVertexAttribArray(normalHandle)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertices.size / 6)
    }
}
