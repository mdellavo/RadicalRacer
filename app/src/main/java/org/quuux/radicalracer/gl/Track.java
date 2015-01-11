package org.quuux.radicalracer.gl;


import android.content.Context;
import android.graphics.YuvImage;
import android.opengl.GLES20;
import android.os.SystemClock;

import org.quuux.radicalracer.R;
import org.quuux.radicalracer.utils.GLUtils;
import org.quuux.radicalracer.utils.Log;
import org.quuux.radicalracer.utils.MathUtils;
import org.quuux.radicalracer.utils.ResourceUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Track {

    private static final String TAG = Log.buildTag(Track.class);

    private static final int SIZE = 256;

    private static final int X_RANGE = SIZE;
    private static final int Z_RANGE = SIZE;

    private static final int POSITION_DATA_SIZE_IN_ELEMENTS = 3;
    private static final int NORMAL_DATA_SIZE_IN_ELEMENTS = 3;
    private static final int COLOR_DATA_SIZE_IN_ELEMENTS = 4;
    private static final int FLOATS_PER_VERTEX = POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS;
    private static final int BYTES_PER_FLOAT = 4;
    private static final int BYTES_PER_SHORT = 2;
    private static final int STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT;

    private static final String POSITION_ATTRIBUTE = "a_Position";
    private static final String NORMAL_ATTRIBUTE = "a_Normal";
    private static final String COLOR_ATTRIBUTE = "a_Color";
    private static final String MVP_MATRIX_UNIFORM = "u_MVPMatrix";

    private static final int NUM_STRIPS = Z_RANGE - 1;
    private static final int NUM_DEGENERATES = 2 * (NUM_STRIPS - 1);
    private static final int VERTICIES_PER_STRIP = 2 * X_RANGE;

    private static final float X_MIN = -512;
    private static final float X_MAX = 512;

    private static final float Z_MIN = 0;
    private static final float Z_MAX = -1024;

    private final FloatBuffer mVertextBuffer;
    private final float[] mHeightMap = new float[X_RANGE * Z_RANGE * FLOATS_PER_VERTEX];
    private final short[] mIndexes = new short[(VERTICIES_PER_STRIP * NUM_STRIPS) + NUM_DEGENERATES];
    private final int mProgram;
    private final ShortBuffer mIndexBuffer;
    private final int[] mVbo = new int[1];
    private final int[] mIbo = new int[1];

    private int mPositionAttribute;
    private int mNormalAttribute;
    private int mColorAttribute;
    private int mMVPMatrixHandle;

    public Track(final Context context) {

        mVertextBuffer = ByteBuffer
               .allocateDirect(mHeightMap.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
               .asFloatBuffer();

        mIndexBuffer = ByteBuffer
                .allocateDirect(mIndexes.length * BYTES_PER_SHORT).order(ByteOrder.nativeOrder())
                .asShortBuffer();

        int vertexShader = GLUtils.loadShader(GLES20.GL_VERTEX_SHADER, ResourceUtils.readTextFile(context, R.raw.ground_vertex_shader));
        int fragmentShader = GLUtils.loadShader(GLES20.GL_FRAGMENT_SHADER, ResourceUtils.readTextFile(context, R.raw.ground_fragment_shader));

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        GLES20.glGenBuffers(1, mVbo, 0);
        GLES20.glGenBuffers(1, mIbo, 0);
    }

    private float[] COLOR_GRASS_LIGHT = {0, .8f, 0};
    private float[] COLOR_GRASS_DARK = {0, .6f, 0};
    private float[][] COLORS_GRASS = { COLOR_GRASS_DARK, COLOR_GRASS_LIGHT} ;

    private float[] COLOR_ROAD_LIGHT = {.6f, .6f, .6f};
    private float[] COLOR_ROAD_DARK = {.4f, .4f, .4f};
    private float[][] COLORS_ROAD = { COLOR_ROAD_DARK, COLOR_ROAD_LIGHT} ;

    private float[] COLOR_STRIPE_LIGHT = { 1, 1, 1};
    private float[] COLOR_STRIPE_DARK = { 1, 0, 0};
    private float[][] COLORS_STRIPE = { COLOR_STRIPE_DARK, COLOR_STRIPE_LIGHT} ;

    private float[] COLOR_ROAD_STRIPE_LIGHT = { 1, 1, 1};
    private float[] COLOR_ROAD_STRIPE_DARK = { .4f, .4f, .4f};
    private float[][] COLORS_ROAD_STRIPE = { COLOR_ROAD_STRIPE_DARK, COLOR_ROAD_STRIPE_LIGHT} ;

    private float[] alternate(float[][] colors, int z) {
        return (z / 32) % 2 == 0 ? colors[0] : colors[1];
    }

    private float[] color(final float x, final float z) {
        float[][] colors;

        final float absX = Math.abs(x);

        if (absX > 16 && absX < 24) {
            colors = COLORS_ROAD_STRIPE;
        } else if (absX < 64) {
            colors = COLORS_ROAD;
        } else if (absX < 80) {
            colors = COLORS_STRIPE;
        } else {
            colors = COLORS_GRASS;
        }

        return alternate(colors, Math.abs((int)z));
    }

    public void render() {

        int offset = 0;
        for (int z=0; z<Z_RANGE; z++) {
            for (int x=0; x<X_RANGE; x++) {
                final float posX = MathUtils.lerp(X_MIN, X_MAX, x/(float)(X_RANGE - 1));
                final float posZ = MathUtils.lerp(Z_MIN, Z_MAX, 1 - (z/(float)(Z_RANGE - 1)));
                mHeightMap[offset++] = posX;
                mHeightMap[offset++] = 0;
                mHeightMap[offset++] = posZ;

                mHeightMap[offset++] = 0;
                mHeightMap[offset++] = 1;
                mHeightMap[offset++] = 0;

                final float[] color = color(posX, posZ);

                mHeightMap[offset++] = color[0];
                mHeightMap[offset++] = color[1];
                mHeightMap[offset++] = color[2];
                mHeightMap[offset++] = 1;

            }
        }

        mVertextBuffer.put(mHeightMap).position(0);

        offset = 0;
        for (int z = 0; z < Z_RANGE - 1; z++) {
            if (z > 0) {
                // Degenerate begin: repeat first vertex
                mIndexes[offset++] = (short) (z * Z_RANGE);
            }

            for (int x = 0; x < X_RANGE; x++) {
                // One part of the strip
                mIndexes[offset++] = (short) ((z * Z_RANGE) + x);
                mIndexes[offset++] = (short) (((z + 1) * Z_RANGE) + x);
            }

            if (z < Z_RANGE - 2) {
                // Degenerate end: repeat last vertex
                mIndexes[offset++] = (short) (((z + 1) * Z_RANGE) + (X_RANGE - 1));
            }
        }

        mIndexBuffer.put(mIndexes).position(0);

    }

    public void draw(float[] mvpMatrix) {
        render();

        GLES20.glUseProgram(mProgram);

        mPositionAttribute = GLES20.glGetAttribLocation(mProgram, POSITION_ATTRIBUTE);
        mNormalAttribute = GLES20.glGetAttribLocation(mProgram, NORMAL_ATTRIBUTE);
        mColorAttribute = GLES20.glGetAttribLocation(mProgram, COLOR_ATTRIBUTE);
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, MVP_MATRIX_UNIFORM);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVbo[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, mVertextBuffer.capacity() * BYTES_PER_FLOAT, mVertextBuffer, GLES20.GL_DYNAMIC_DRAW);

        GLES20.glVertexAttribPointer(mPositionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false, STRIDE, 0);
        GLES20.glEnableVertexAttribArray(mPositionAttribute);

        GLES20.glVertexAttribPointer(mNormalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false, STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT);
        GLES20.glEnableVertexAttribArray(mNormalAttribute);

        GLES20.glVertexAttribPointer(mColorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false, STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
        GLES20.glEnableVertexAttribArray(mColorAttribute);

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIbo[0]);
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndexBuffer.capacity() * BYTES_PER_SHORT, mIndexBuffer, GLES20.GL_STREAM_DRAW);

        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mIndexes.length, GLES20.GL_UNSIGNED_SHORT, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);


    }
}