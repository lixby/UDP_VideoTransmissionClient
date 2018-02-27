package com.skylight.client.tcp.handler;

import com.skylight.client.tcp.TcpCommandClient;
import com.skylight.command.PacketCmdHandler;

/**
 * Description:
 * Author: Created by lixby on 17-12-20.
 */

public class StreamCmdHandler extends PacketCmdHandler<byte[]> {


    public StreamCmdHandler(){
        setPacketType(PACKET_QUEST);

    }

    @Override
    public void createMessage(byte[] msg) {
        setMessage(new byte[0]);
    }

    @Override
    public void execute() {
        TcpCommandClient tcpClient=getTcpClient();
        if(tcpClient!=null){
            byte[] packet=createSendPacket(getMessage());
            if(getStatus()==CMD_STOP_EXECUTE){
                return;
            }
            tcpClient.sendCommand(packet);

        }

    }


}
