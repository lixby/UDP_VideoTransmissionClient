package com.skylight.client.tcp.handler;


import com.skylight.client.tcp.TcpCommandClient;
import com.skylight.client.tcp.TcpCommandType;
import com.skylight.client.tcp.mode.TlvSAKMode;
import com.skylight.command.PacketCmdHandler;

/**
 * Description:
 * Author: Created by lixby on 17-12-19.
 */

public class UdpPacketSAKHandler extends PacketCmdHandler<byte[]> {

    private static final String TAG="UdpPacketSAKHandler";

    private TlvSAKMode tlv;

    public UdpPacketSAKHandler(){
        setCommandType(TcpCommandType.COMMAND_SYNC_WIN_INFRO);
        setPacketType(PACKET_RESPONSE);

    }

    public UdpPacketSAKHandler(TlvSAKMode tlv){
        this.tlv=tlv;
        setCommandType(TcpCommandType.COMMAND_SYNC_WIN_INFRO);
        setPacketType(PACKET_RESPONSE);

    }

    public byte[] createSAK(){
        byte[] parseData=tlv.createContent();
        setMessage(parseData);
        return  createSendPacket(parseData);
    }

    @Override
    public void createMessage(byte[] bytes) {

    }

    @Override
    public void execute() {
        if(tlv!=null){
            byte[] parseData=tlv.createContent();
            if(parseData!=null){
                setMessage(parseData);
                byte[] result= createSendPacket(parseData);
                TcpCommandClient tcpClient=getTcpClient();
                if(tcpClient!=null){
                    tcpClient.sendCommand(result);
                }
            }
        }
    }





}
