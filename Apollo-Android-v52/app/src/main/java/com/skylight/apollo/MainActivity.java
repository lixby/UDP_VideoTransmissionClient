package com.skylight.apollo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.kandaovr.sdk.renderer.StitchRenderer;
import com.kandaovr.sdk.util.LifeTimeMonitor;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = (Button)findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StitchActivity.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mButton.setEnabled(false);
                    }
                });
                startActivity(intent);
            }
        });

        registerReceiver(mStitchRendererDestroy, new IntentFilter(StitchRenderer.BROADCAST_DESTROY));
    }

    private BroadcastReceiver mStitchRendererDestroy = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mButton.setEnabled(true);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStitchRendererDestroy);
    }
}
