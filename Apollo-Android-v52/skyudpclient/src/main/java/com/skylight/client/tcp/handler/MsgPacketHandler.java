package com.skylight.client.tcp.handler;

import android.util.Log;
import com.skylight.client.tcp.TcpCommandClient;
import com.skylight.command.PacketCmdHandler;

/**
 * Description:
 * Author: Created by lixby on 17-12-15.
 */

public class MsgPacketHandler extends PacketCmdHandler<String> {

    public MsgPacketHandler(){

    }

    @Override
    public void createMessage(String msg) {
        setMessage(msg.getBytes());
    }

    @Override
    public void execute() {
        TcpCommandClient tcpClient=getTcpClient();
        if(tcpClient!=null){
            Log.i(TAG,"sendCommand");
            byte[] result=createSendPacket(getMessage());
            if(getStatus()==CMD_STOP_EXECUTE){
                return;
            }
            tcpClient.sendCommand(result);

        }

    }

}
