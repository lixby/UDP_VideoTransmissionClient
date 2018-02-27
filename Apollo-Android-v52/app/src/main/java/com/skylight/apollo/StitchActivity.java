package com.skylight.apollo;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import com.skylight.apollo.encoder.EncodeFormat;
import com.kandaovr.sdk.renderer.Renderer;
import com.kandaovr.sdk.renderer.StitchRenderer;
import com.kandaovr.sdk.view.RenderView;
import com.skylight.apollo.decoder.DataDecoder;
import com.skylight.apollo.decoder.TestDataDecoder;
import com.skylight.apollo.decoder.UsbDataDecoder;
import com.skylight.apollo.decoder.util.InfoExtractor;
import com.skylight.apollo.encoder.VideoEncoder;
import com.skylight.apollo.util.Constants;
import com.skylight.apollo.util.Util;
import com.skylight.client.tcp.handlercallback.StreamCmdHandlerCallback;
import com.skylight.client.tcp.handlercallback.TcpIpStatusHandlerCallback;
import com.skylight.client.udp.StatisticsProcessor;
import com.skylight.command.callback.CmdStatus;
import com.skylight.command.callback.ICmdHandlerCallback;
import com.skylight.mode.WifiCameraMode;
import com.skylight.mode.camera.ProductionInformation;
import com.skylight.util.Logger;

import java.io.File;
import java.io.FileOutputStream;

public class StitchActivity extends AppCompatActivity {

    private final static String TAG = "StitchActivity";
    private static final boolean DEBUG_LIFE_TIME = true;
    private static final boolean DEBUG_CODEC_CRUSH = true;
    private final static int SAMPLE_RATE = 44100;
    private final static boolean RECORD_RAW_DATA = true && Constants.DEBUG_MODE;
    private final static boolean USE_PHONE_MIC = false;

    private boolean mRecordRawStarted = false;

    private RenderView mMainView;
    private StitchRenderer mStitchRenderer;
    private DataDecoder mDecoder;

    private boolean mRecording = false;
    private String mRecordingPath = null;
    private EncodeFormat mRecordingConfig = null;
    private VideoEncoder mVideoEncoder = new VideoEncoder();

    private NotificationManager nm;
    private boolean mLensParamSet = false;
    private Bitmap mBackground = null;
    private final Handler mHandler = new Handler();

    // buttons
    private Button startStream;
    private Button stopStream;
    private Button mRecordButton;
    private Button mStreamButton;
    private Button mAdjustCalibrationButton;
    private TextView label2;

    Object mutexObj = new Object();


    private WifiCameraMode wifiCameraMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "dsj stitch activity create");
        Util.verifyStoragePermissions(this);
        Util.createFolders();
        setContentView(R.layout.activity_stitch);
        setTitle(R.string.activity_stitch);

        label2 = (TextView) findViewById(R.id.label2);

        // create renderer
        mStitchRenderer = new StitchRenderer(this, 3840, 1920, StitchRenderer.CAMERA_REAR);

        // create decoder
        if(Constants.DEBUG_MODE){
            mDecoder = new TestDataDecoder(this);
        }
        else{
            mDecoder = new UsbDataDecoder(this);
            // start skyline data service
            initService();
        }

        // load background for 180 view
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        mBackground = BitmapFactory.decodeResource(getResources(), R.raw.background1, options);

        // set renderer
        mStitchRenderer.setFov(120);
        mStitchRenderer.setCalibrateInterval(10000);
        mStitchRenderer.setThumbnailEnabled(true);
        mStitchRenderer.setBackground(mBackground);
        mStitchRenderer.setTextureSurfaceListener(new Renderer.TextureSurfaceListener() {
            @Override
            public void onSurfaceCreated(Surface surface) {
                Log.e("lixby", "onSurfaceCreated--surface="+surface);
                mDecoder.setSurface(surface);
            }
        });

        mStitchRenderer.setOnVideoFrameAvailableListener(new StitchRenderer.OnVideoFrameAvailableListener() {
            @Override
            public void onVideoTextureAvailable(int textureId) {
                mVideoEncoder.setTextureId(textureId);
            }

            @Override
            public void onVideoFrameAvailable() {
                mVideoEncoder.onVideoFrame();
            }
        });

        // create SurfaceView
        mMainView = (RenderView)findViewById(R.id.surfaceView);
        mMainView.setRenderer(mStitchRenderer);
        mMainView.setGestureListener(mStitchRenderer);

        if(mDecoder instanceof TestDataDecoder || RECORD_RAW_DATA || Constants.DEBUG_MODE) {
            mStitchRenderer.setLensParams(Constants.TEST_LENS_PARAM);
        }else{
            setLensParams(Constants.TEST_LENS_PARAM);
        }

        initComponent();
        updateLayout(getResources().getConfiguration());

        mRecordingConfig = new EncodeFormat();
        mRecordingConfig.width = 2160;
        mRecordingConfig.height = 1080;
        mRecordingConfig.videoBitrate = 2*1000000; // 1M
        mRecordingConfig.channels = 2; // must be 2
        mRecordingConfig.sampleRate = 44100; // must be 44100
        mRecordingConfig.audioBitrate = 88200;

        inItWifiCameraMode();
    }

    private void inItWifiCameraMode(){
        wifiCameraMode=new WifiCameraMode();
        wifiCameraMode.initialized(new Intent(),new ProductionInformation(),new TcpIpStatusHandlerCallback(){

            @Override
            public void inItSuccess() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StitchActivity.this, "Init success", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void inItFailed() {

            }

            @Override
            public void disConnected() {

            }

            @Override
            public void responseStatus(CmdStatus status) {

            }
        });

        startStream= (Button) findViewById(R.id.startStream);
        stopStream= (Button) findViewById(R.id.stopStream);

        startStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startWifiModeStream();
            }
        });

        stopStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopWifiModeStream();
            }
        });
    }

    StatisticsProcessor.StatisticsListener statisticsListener=new StatisticsProcessor.StatisticsListener() {

        @Override
        public void statisticalResult(final float lossRate, final float bandwidth) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    label2.setText("丢包率："+lossRate+"% , UDP带宽："+bandwidth+"KB/S");
                }
            });

        }
    };

    private void startWifiModeStream(){
        wifiCameraMode.setStatisticsListener(statisticsListener);
        wifiCameraMode.obtainStream(new StreamCmdHandlerCallback() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void responseReadFrame(int channel, byte[] data, int length, long pts) {
                Log.i("lixby", "channel="+channel + " |data-length = " + data.length+"|length= "+length);
                showMessage("responseReadFrame " + channel + " length = " + length);
                if(channel == 10) {
                    try {
                        //byte[] frame = frameDataFilter(data, length);
                        if (mDecoder instanceof UsbDataDecoder) {
                            ((UsbDataDecoder) mDecoder).onReadFrame(data, data.length);
                        }
                        // TODO remove the following before release
                        if (RECORD_RAW_DATA) {
                            if(InfoExtractor.isIFrame(data)){
                                mRecordRawStarted = true;
                            }
                            if(mRecordRawStarted) {
                                recordData(data);
                            }
                        }
                    }catch(final Exception ex){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(StitchActivity.this, "exception 0: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        ex.printStackTrace();
                    }
                }else if(channel == 20){
                    if (RECORD_RAW_DATA) {
                    }

                    try {
                        mVideoEncoder.onAudioFrame(data, length, SAMPLE_RATE);
                    }catch(final Exception ex){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(StitchActivity.this, "exception 1: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        ex.printStackTrace();
                    }

                }

            }

            @Override
            public void responseStatus(CmdStatus status) {
                String mse="";
                if(status.getCode()==0){
                    mse="Start stream success";
                }else{
                    mse="Start stream fail";
                }

                final String finalMse = mse;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StitchActivity.this, finalMse, Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

        mDecoder.startDecoding();
        mStitchRenderer.resume();
    }

    public byte[] frameDataFilter(byte[] data, int length) {
        byte[] frame1;
        if((data[10] & 31) == 7) {
            frame1 = new byte[length - 6];
            System.arraycopy(data, 6, frame1, 0, length - 6);
        } else {
            frame1 = new byte[length - 24];
            System.arraycopy(data, 24, frame1, 0, length - 24);
        }

        return frame1;
    }

    private void setLensParams(String lensParam){
        if(!mLensParamSet){
            if(Util.isLenParamValid(lensParam) && mStitchRenderer != null) {
                mStitchRenderer.setLensParams(lensParam);
                mLensParamSet = true;
            }
        }

    }

    private void stopWifiModeStream(){
        wifiCameraMode.setStatisticsListener(null);
        wifiCameraMode.releaseStream(new ICmdHandlerCallback() {
            @Override
            public void responseStatus(CmdStatus status) {
                String mse="";
                if(status.getCode()==0){
                    mse="Stop stream success";
                }else{
                    mse="Stop stream fail";
                }

                final String finalMse = mse;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(StitchActivity.this, finalMse, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        mDecoder.stopDecoding();
        mStitchRenderer.pause();
    }

    private void releaseWifiCameraMode(){
        if(wifiCameraMode!=null){
            wifiCameraMode.destroy();

        }
    }


    private void initComponent(){
        // Record video button
        mRecordButton = (Button)findViewById(R.id.recordButton);
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Button button = (Button)view;
                if(!mRecording){
                    disableButtons(mRecordButton);
                    button.setText(R.string.stitch_record_stop);

                    mRecording = true;
                    mRecordingPath = Util.VIDEO_DIR + "/" + System.currentTimeMillis() + ".mp4";
                    mVideoEncoder.startRecord(mRecordingPath, mRecordingConfig, new VideoEncoder.RecordErrorListener() {
                        @Override
                        public void onError(String recordPath, final Exception exception) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(StitchActivity.this, "Record Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });

                    if(RECORD_RAW_DATA) {
                        mRecordRawStarted = false;
                        openFile("data" + System.currentTimeMillis() + ".h264");
                    }
                }
                else{
                    stopRecording();

                    // TODO remove the following before release
                    if(RECORD_RAW_DATA) {
                        closeFile();
                    }
                }
            }
        });

        // Streaming button
        mStreamButton = (Button)findViewById(R.id.streamButton);
        mStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Button button = (Button)view;
                if(!mRecording){
                    disableButtons(mStreamButton);
                    button.setText(R.string.stitch_record_stop);

                    mRecording = true;
                    mVideoEncoder.startRecord(Constants.RTMP_ENDPOINT, mRecordingConfig, new VideoEncoder.RecordErrorListener() {
                        @Override
                        public void onError(String recordPath, final Exception exception) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(StitchActivity.this, "Streaming Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                }
                else{
                    stopRecording();
                }
            }
        });

        Button frontCameraButton = (Button)findViewById(R.id.frontCameraButton);
        frontCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStitchRenderer.setCameraPosition(StitchRenderer.CAMERA_FRONT);
                mStitchRenderer.setCameraActive(StitchRenderer.CAMERA_FRONT);
            }
        });

        Button rearCameraButton = (Button)findViewById(R.id.rearCameraButton);
        rearCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStitchRenderer.setCameraPosition(StitchRenderer.CAMERA_REAR);
                mStitchRenderer.setCameraActive(StitchRenderer.CAMERA_REAR);
            }
        });

        Button full360Button = (Button)findViewById(R.id.full360Button);
        full360Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mStitchRenderer.setCameraPosition(StitchRenderer.CAMERA_FRONT);
                mStitchRenderer.setCameraActive(StitchRenderer.CAMERA_BOTH);
            }
        });

        mAdjustCalibrationButton = (Button)findViewById(R.id.adjustCalibrationButton);
        mAdjustCalibrationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean enabled = !mStitchRenderer.isAdjustCalibrationEnabled();
                mStitchRenderer.setAdjustCalibrationEnabled(enabled);
                mAdjustCalibrationButton.setText(enabled ? "Stop Auto Calibration" : "Start Auto Calibration");
            }
        });

        // buttons for setting orientation
        LinearLayout orientationLayout = (LinearLayout)findViewById(R.id.orientationLayout);
        int count = orientationLayout.getChildCount();
        for(int i = 0 ; i < count; i ++){
            final Button button = (Button)orientationLayout.getChildAt(i);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    float x = Float.parseFloat((String)button.getTag());
                    mStitchRenderer.setOrientation(x, 0);
                }
            });
        }
    }

    private void stopRecording(){
        final Button button = mRecordButton.isEnabled() ? mRecordButton : mStreamButton;
        mRecording = false;
        mVideoEncoder.stopRecord(new VideoEncoder.RecordCompleteListener() {
            @Override
            public void onComplete(final String recordPath) {
                if(button == mRecordButton){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(StitchActivity.this, "Saved Video " + recordPath, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        mMainView.requestRender(); // force one more rendering to save the file. this is needed in case viewfinder freezes
        button.setEnabled(false);
        // delay the UI change by 1 second so that the encoder has sufficient time to shutdown
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                enableButtons();
                button.setText(button == mStreamButton ? R.string.stitch_stream : R.string.stitch_record);
            }
        }, 1000);
    }

    private void disableButtons(Button excluded){
        mRecordButton.setEnabled(mRecordButton == excluded);
        mStreamButton.setEnabled(mStreamButton == excluded);
    }

    private void enableButtons(){
        mRecordButton.setEnabled(true);
        mStreamButton.setEnabled(true);
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

    private void initService(){
        Log.d(TAG, "dsj initService called");
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private void showMessage(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView label = (TextView) findViewById(R.id.label);
                label.setText(message);
            }
        });
    }

    @Override
    protected void onResume(){
        if(DEBUG_LIFE_TIME) {
            Log.d(TAG, "dsj onResume");
        }
        super.onResume();

        if(USE_PHONE_MIC){
            startAudio();
        }
    }

    @Override
    protected void onPause(){
        // stop recording if it is currently recording
        if(mRecording){
            stopRecording();
        }

        /*mDecoder.stopDecoding();
        if (DEBUG_CODEC_CRUSH) {
            Log.d(TAG, "dsj onPause 3");
        }
        mStitchRenderer.pause();*/

        if(USE_PHONE_MIC){
            if (DEBUG_CODEC_CRUSH) {
                Log.d(TAG, "dsj onPause 4");
            }
            stopAudio();
        }
        if (DEBUG_CODEC_CRUSH) {
            Log.d(TAG, "dsj onPause 5");
        }
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        synchronized(mutexObj) {
            releaseWifiCameraMode();
            if (mStitchRenderer != null) {
                Log.d(TAG, "dsj stitchRenderer destroy start");
                mStitchRenderer.destroy();
                mStitchRenderer = null;
                Log.d(TAG, "dsj stitchRenderer destroy end");
            }
            super.onDestroy();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_stitch, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.res:
                mStitchRenderer.setThumbnailEnabled(!mStitchRenderer.isThumbnailEnabled());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Display a list of bit rate options
     * @param item
     */
    private void showBitrateOptions(final MenuItem item){

        final CharSequence labels[] = new CharSequence[] {"512Kbps", "1Mbps", "2Mbps", "3Mbps", "4Mbps", "5Mbps", "8Mbps", "10Mbps"};
        final int[] bitrates = new int[]{512000, 1000000, 2000000, 3000000, 4000000, 5000000, 8000000, 10000000};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick a bitrate");
        builder.setItems(labels, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mRecordingConfig.videoBitrate = bitrates[which];
                item.setTitle(labels[which]);
            }
        });
        builder.show();
    }

    // for recording
    private FileOutputStream outputStream = null;
    public void openFile(String fileName)
    {
        String sdStatus = Environment.getExternalStorageState();
        if (!sdStatus.equals("mounted"))
        {
            Log.d("TestFile", "SD card is not avaiable/writeable right now.");
            return;
        }
        try
        {
            File file = new File(Environment.getExternalStorageDirectory(),
                    fileName);
            if (!file.exists())
            {
                Log.d("TestFile", "Create the file:" + fileName);
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file, true);
        }
        catch (Exception e)
        {
            Log.e("TestFile", "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void recordData(byte[] data){
        if(outputStream == null){
            return;
        }

        try
        {
            outputStream.write(data, 0 ,data.length);
        }
        catch (Exception e)
        {
            Log.e("TestFile", "Error on writeFilToSD.");
            e.printStackTrace();
        }
    }

    public void closeFile(){
        try
        {
            if(outputStream != null){
                outputStream.close();
            }
        }
        catch(Exception e){
            Log.e("UsbActivity", "Error closing file: " + e.getMessage());
            e.printStackTrace();
        }
        finally {
            outputStream = null;
        }
    }

    private AudioRecord mic = null;
    private Thread aworker = null;
    private boolean aloop = false;
    private void startAudio(){
        try {
            mic = chooseAudioRecord();
            if (mic == null) {
                Toast.makeText(this, "Cannot open mic for audio recording", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch(Exception e){
            Toast.makeText(this, "Cannot open mic for audio recording", Toast.LENGTH_SHORT).show();
            return;
        }

        aworker = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(5);//android.os.Process.THREAD_PRIORITY_AUDIO);
                mic.startRecording();

                byte pcmBuffer[] = new byte[2048];
                while (aloop && !Thread.interrupted()) {
                    int size = mic.read(pcmBuffer, 0, pcmBuffer.length);
                    if (size <= 0) {
                        break;
                    }
                    if(mStitchRenderer != null) {
                        mVideoEncoder.onAudioFrame(pcmBuffer, size, SAMPLE_RATE);
                    }
                }
            }
        });
        aloop = true;
        aworker.start();
    }

    private void stopAudio(){
        aloop = false;
        if (aworker != null) {
            aworker.interrupt();
            try {
                aworker.join();
            } catch (InterruptedException e) {
                aworker.interrupt();
            }
            aworker = null;
        }

        if (mic != null) {
            mic.setRecordPositionUpdateListener(null);
            mic.stop();
            mic.release();
            mic = null;
        }
    }

    private AudioRecord chooseAudioRecord() {
        int sampleRate = SAMPLE_RATE;
        int minBufferSize = 2048;// AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord mic = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        if (mic.getState() != AudioRecord.STATE_INITIALIZED) {
            mic = null;
        }

        return mic;
    }
}
