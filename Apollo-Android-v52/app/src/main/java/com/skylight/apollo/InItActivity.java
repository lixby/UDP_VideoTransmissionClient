package com.skylight.apollo;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.SharedPreferencesCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.skylight.apollo.util.SharedPreferencesCache;
import com.skylight.client.tcp.mode.TcpIpInformation;

/**
 * Description:
 * Author: Created by lixby on 18-1-12.
 */

public class InItActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.init_layout);
        inItView();



    }

    private void inItView(){
        final EditText editText= (EditText) findViewById(R.id.editText3);
        Button   ok= (Button) findViewById(R.id.button3);
        String tcpSerIP=SharedPreferencesCache.get(getApplicationContext(),SharedPreferencesCache.TCPSERVER_IP);
        if(!TextUtils.isEmpty(tcpSerIP)){
            editText.setText(tcpSerIP);
        }

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String server_IP=editText.getText().toString();
                if(!TextUtils.isEmpty(server_IP)){
                    TcpIpInformation.getInstance().setServerTcp_IP(server_IP);
                    SharedPreferencesCache.put(getApplicationContext(),SharedPreferencesCache.TCPSERVER_IP,server_IP);
                }

                Intent intent=new Intent(InItActivity.this,StitchActivity.class);
                startActivity(intent);

            }
        });


    }


}
