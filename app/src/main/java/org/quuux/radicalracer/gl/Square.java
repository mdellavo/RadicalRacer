package org.quuux.radicalracer.gl;

import android.opengl.GLES20;

import org.quuux.radicalracer.utils.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Square {

    private static final int COORDS_PER_VERTEX = 3;
    private static final float SQUARE_COORDS[] = {
            -0.5f,  0.5f, 0.0f,   // top left
            -0.5f, -0.5f, 0.0f,   // bottom left
             0.5f, -0.5f, 0.0f,   // bottom right
             0.5f,  0.5f, 0.0f }; // top right

    private static final short DRAW_ORDER[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

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

    private final int mProgram;
    private final FloatBuffer mVertexBuffer;
    private final ShortBuffer mDrawListBuffer;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private final float[] mColor;

    public Square(float[] color) {
        mColor = color;

        ByteBuffer bb = ByteBuffer.allocateDirect(
                SQUARE_COORDS.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(SQUARE_COORDS);
        mVertexBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(DRAW_ORDER.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(DRAW_ORDER);
        mDrawListBuffer.position(0);

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

        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                VERTEX_STRIDE, mVertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        GLES20.glUniform4fv(mColorHandle, 1, mColor, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLUtils.checkGlError("glGetUniformLocation");

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        GLUtils.checkGlError("glUniformMatrix4fv");

        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, DRAW_ORDER.length,
                GLES20.GL_UNSIGNED_SHORT, mDrawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}