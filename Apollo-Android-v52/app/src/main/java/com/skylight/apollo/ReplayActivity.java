package com.skylight.apollo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.kandaovr.sdk.renderer.Renderer;
import com.kandaovr.sdk.renderer.ReplayRenderer;
import com.kandaovr.sdk.renderer.StitchRenderer;
import com.kandaovr.sdk.util.Constants;
import com.kandaovr.sdk.view.RenderView;
import com.skylight.apollo.decoder.DataDecoder;
import com.skylight.apollo.decoder.ImageDataDecoder;
import com.skylight.apollo.decoder.VideoDataDecoder;
import com.skylight.apollo.util.Util;

public class ReplayActivity extends AppCompatActivity{
    private static final boolean DEBUG_GYRO_INIT = false;
    private final static String TAG = "ReplayActivity";
    private final static int[] MODE_TEXTS = new int[]{
            R.string.replay_mode_hand, R.string.replay_mode_gyro
    };
    private final static int[] PROJECTION_TEXTS = new int[]{
            R.string.replay_projection_perspective, R.string.replay_projection_fisheye,
            R.string.replay_projection_planet
    };

    RenderView mMainView;
    ReplayRenderer mReplayRenderer;
    DataDecoder mDecoder;

//    private int mMode = ReplayRenderer.MODE_HAND; // default mode
    private int mMode = ReplayRenderer.MODE_GYRO;
    private int mProjection = ReplayRenderer.PROJECTION_PERSPECTIVE; // default projection

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "dsj replayActivity create");
        super.onCreate(savedInstanceState);
        Util.verifyStoragePermissions(this);
        setContentView(R.layout.activity_replay);
        setTitle(R.string.activity_replay);

        int mediaType = getIntent().getIntExtra("mediaType", Constants.MEDIA_TYPE_VIDEO);
        String mediaPath = getIntent().getStringExtra("mediaPath");
        if (mediaPath == null) { // TODO for testing
            mediaPath = Util.VIDEO_DIR + "/1496396904699.mp4";
//            mediaPath = Util.PICTURE_DIR + "/1474627572157.jpg";
//            mediaType = Constants.MEDIA_TYPE_PICTURE;
        }

        // create decoder
        if (mediaType == Constants.MEDIA_TYPE_VIDEO) {
            mDecoder = new VideoDataDecoder(this, mediaPath);
        } else {
            mDecoder = new ImageDataDecoder(this, mediaPath);
        }

        // create renderer
        mReplayRenderer = new ReplayRenderer(this);
        mReplayRenderer.setFov(70);
        if (DEBUG_GYRO_INIT) {
            Log.d(TAG, "dsj before native set mMode mMode = " + mMode);
        }
        mReplayRenderer.setMode(mMode);
        if (DEBUG_GYRO_INIT) {
            Log.d(TAG, "dsj after native set mMode mMode = " + mMode);
        }
        mReplayRenderer.setTextureSurfaceListener(new Renderer.TextureSurfaceListener() {
            @Override
            public void onSurfaceCreated(Surface surface) {
                mDecoder.setSurface(surface);
            }
        });
//        mReplayRenderer.setMode(mMode);

        // create SurfaceView
        mMainView = (RenderView) findViewById(R.id.surfaceView);
        mMainView.setRenderer(mReplayRenderer);
        mMainView.setGestureListener(mReplayRenderer);

        // Capture button
        TextView captureButton = (TextView) findViewById(R.id.captureButton);
        captureButton.setClickable(true);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mReplayRenderer.capture(Util.SCREENSHOT_DIR + "/" + System.currentTimeMillis() + ".jpg",
                        new StitchRenderer.CaptureListener() {
                            @Override
                            public void onComplete(final String path) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(ReplayActivity.this, "Save Screenshot " + path, Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                            @Override
                            public void onError(String path, String errorMessage) {
                                Toast.makeText(ReplayActivity.this, "Capture Error:" + errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        // Change projection method button
        TextView projectionButton = (TextView) findViewById(R.id.projectionButton);
        projectionButton.setClickable(true);
        projectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView button = (TextView) view;
                mProjection = (mProjection + 1) % PROJECTION_TEXTS.length;
                button.setText(PROJECTION_TEXTS[mProjection]);

                // set projection
                mReplayRenderer.setProjection(mProjection);
            }
        });

        // Change navigation method button
        TextView modeButton = (TextView) findViewById(R.id.modeButton);
        modeButton.setText(MODE_TEXTS[mMode]);
        modeButton.setClickable(true);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView button = (TextView) view;
                mMode = (mMode + 1) % MODE_TEXTS.length;
                button.setText(MODE_TEXTS[mMode]);

                // set mode
                mReplayRenderer.setMode(mMode);
            }
        });

        // start sensor tracker
        updateLayout(getResources().getConfiguration());
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            updateLayout(newConfig);
    }

    private void updateLayout(Configuration configuration){
        // Checks the orientation of the screen
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getSupportActionBar().hide();
        } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT){
            getSupportActionBar().show();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        mDecoder.startDecoding();
        mReplayRenderer.resume();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mDecoder.stopDecoding();
        mReplayRenderer.pause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_replay, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.fov:
                showFovOptions(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFovOptions(final MenuItem item){

        final String labels[] = new String[] {"60", "80", "100", "120", "140"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick an action");
        builder.setItems(labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mReplayRenderer.setFov(Float.parseFloat(labels[which]));
            }
        });
        builder.show();
    }

}
