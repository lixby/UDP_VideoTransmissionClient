package com.skylight.client.tcp.handler;

import android.util.Log;
import com.skylight.client.tcp.TcpCommandClient;
import com.skylight.client.tcp.TcpCommandType;
import com.skylight.command.PacketCmdHandler;

/**
 * Description:
 * Author: Created by lixby on 17-12-21.
 */

public class TcpInitCmdHandler extends PacketCmdHandler<byte[]> {

    public TcpInitCmdHandler(){
        setCommandType(TcpCommandType.COMMAND_UDP_IPPORT);
        setPacketType(PACKET_QUEST);

    }

    @Override
    public void createMessage(byte[] bytes) {
        setMessage(bytes);
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
