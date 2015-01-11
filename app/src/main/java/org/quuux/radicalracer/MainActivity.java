package org.quuux.radicalracer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import org.quuux.radicalracer.gl.Track;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new GameView(this));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GameView extends GLSurfaceView {

        public GameView(final Context context) {
            super(context);
            setEGLContextClientVersion(2);
            setRenderer(new RaceRenderer());
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        }
    }

    private class RaceRenderer implements GLSurfaceView.Renderer {

        private final float[] mMVPMatrix = new float[16];
        private final float[] mProjectionMatrix = new float[16];
        private final float[] mViewMatrix = new float[16];
        private final float[] mRotationMatrix = new float[16];
        private final float[] mScratch = new float[16];

        private Track mGround;

        @Override
        public void onSurfaceCreated(final GL10 unused, final EGLConfig config) {
            GLES20.glClearColor(0.6f, 0.6f, 1.0f, 1.0f);

            mGround = new Track(MainActivity.this);
        }

        @Override
        public void onSurfaceChanged(final GL10 unused, final int width, final int height) {
            GLES20.glViewport(0, 0, width, height);

            float ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1.5f, 1024);
        }

        @Override
        public void onDrawFrame(final GL10 unused) {
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            Matrix.setLookAtM(
                    mViewMatrix, 0,
                    0, 128, 128,
                    0, 0, -512,
                    0, 1, 0
            );

            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

            mGround.draw(mMVPMatrix);
        }
    }
}
