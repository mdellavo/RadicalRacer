package org.quuux.radicalracer.gl;

import android.opengl.GLES20;

import org.quuux.radicalracer.utils.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Triangle {

    private static final int COORDS_PER_VERTEX = 3;

    private static final float TRIANGLE_COORDS[] = {   // in counterclockwise order:
            0.0f,  0.622008459f, 0.0f,                // top
            -0.5f, -0.311004243f, 0.0f,               // bottom left
            0.5f, -0.311004243f, 0.0f                 // bottom right
    };

    private static final float COLOR[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}";


    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}";

    private static final int VERTEXT_COUNT = TRIANGLE_COORDS.length / COORDS_PER_VERTEX;
    private static final int VERTEXT_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    private final int mProgram;
    private final FloatBuffer mVertexBuffer;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    public Triangle() {
        ByteBuffer bb = ByteBuffer.allocateDirect(TRIANGLE_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());

        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(TRIANGLE_COORDS);
        mVertexBuffer.position(0);

        int vertexShader = GLUtils.loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);
    }

    public void draw(float[] mvpMatrix) {
        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        GLES20.glEnableVertexAttribArray(mPositionHandle);

        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VERTEXT_STRIDE, mVertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        GLES20.glUniform4fv(mColorHandle, 1, COLOR, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLUtils.checkGlError("glGetUniformLocation");

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtils.checkGlError("glUniformMatrix4fv");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEXT_COUNT);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}