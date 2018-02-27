package com.skylight.mode;

import android.content.Intent;
import com.skylight.client.tcp.handler.TcpInitCmdHandler;
import com.skylight.client.tcp.handlercallback.StreamCmdHandlerCallback;
import com.skylight.client.tcp.handlercallback.TcpIpStatusHandlerCallback;
import com.skylight.client.tcp.mode.TlvContentFactory;
import com.skylight.client.tcp.mode.TlvIntegerMode;
import com.skylight.client.tcp.mode.TlvMode;
import com.skylight.client.tcp.mode.TlvModes;
import com.skylight.client.tcp.mode.TlvStringMode;
import com.skylight.client.udp.StatisticsProcessor;
import com.skylight.client.udp.UdpFrameReceiver;
import com.skylight.client.udp.UdpPacketSender;
import com.skylight.client.udp.UdpStreamClient;
import com.skylight.command.TcpCommandManager;
import com.skylight.client.tcp.TcpCommandClient;
import com.skylight.client.tcp.TcpCommandType;
import com.skylight.command.PacketCmdHandler;
import com.skylight.client.tcp.handler.StreamCmdHandler;
import com.skylight.command.callback.ICmdHandlerCallback;
import com.skylight.mode.camera.ProductionInformation;
import com.skylight.util.Logger;

/**
 * Description:
 * Author: Created by lixby on 17-12-18.
 */

public class WifiCameraMode extends CameraMode<Intent,ProductionInformation,TcpIpStatusHandlerCallback>{

    private TcpCommandClient tcpClient;
    private UdpStreamClient udpClient;

    private TcpCommandManager commandManager;
    private UdpFrameReceiver udpFrameReceiver;
    private UdpPacketSender udpPacketSender;

    private ProductionInformation proInfro;
    private TcpIpStatusHandlerCallback tcpInitCallback;

    public WifiCameraMode(){

    }

    @Override
    public  Void initialized(Intent intent, ProductionInformation information,TcpIpStatusHandlerCallback callback) {
        this.proInfro=information;
        this.tcpInitCallback=callback;

        //Init CommandManager
        commandManager=new TcpCommandManager();
        commandManager.startRun();

        //Init tcp
        tcpClient=new TcpCommandClient(tcpStatusCallback);

        //Init udp
        udpClient=new UdpStreamClient();
        udpClient.setStatusCallback(udpStatusCallback);

        //Init udp sender
        udpPacketSender=new UdpPacketSender(udpClient);
        udpFrameReceiver =new UdpFrameReceiver(this);

        return  null;
    }

    public TcpCommandManager getCommandManager() {
        return commandManager;
    }

    public TcpCommandClient getTcpClient() {
        return tcpClient;
    }

    public UdpPacketSender getUdpPacketSender() {
        return udpPacketSender;
    }

    public UdpStreamClient getUdpClient() {
        return udpClient;
    }

    /** release tcp*/
    public void releaseTcp(){
        if(tcpClient!=null){
            tcpClient.setTcpStatusCallback(tcpStatusCallback);
            tcpClient.release();
        }
    }

    /**Tcp initialization status callback*/
    TcpCommandClient.TcpStatusCallback tcpStatusCallback=new TcpCommandClient.TcpStatusCallback() {

        @Override
        public void connected(TcpStatus tcpStatus) {
            if(tcpStatus==TcpStatus.CONNECTION_SUCCEEDED){
                //After TCP initializes and connects successfully,
                //it initializes UDP and obtains the local IP address and Upd port number to the server.
                Logger.i("Tcp connected");
                startUdp();
            }else if(tcpStatus==TcpStatus.CONNECTION_FAILED) {
                //TCP initializes failed
                tcpInitCallback.inItFailed();
            }else{
                //Disconnected from the server
                Logger.e("Tcp disconnected");
                tcpInitCallback.disConnected();
                destroy();
            }

        }

        @Override
        public void outPutData(byte[] packet) {
            if(commandManager!=null){
                commandManager.getTcpReceiver().addRecPacket(packet);
            }


        }
    };

    /**start run udp*/
    private void startUdp(){
        if(udpClient!=null){
            udpClient.startRun();
        }

    }

    /**stop run udp*/
    private void stopUdp(){
        if(udpClient!=null){
            udpClient.stopRun();
        }
    }

    /** release udp*/
    private void releaseUdp(){
        stopUdp();
        if(udpClient!=null){
            udpClient.setStatusCallback(null);
            udpClient.release();
        }
    }

    /**Udp initialization status callback*/
    UdpStreamClient.UDPStatusCallback udpStatusCallback=new UdpStreamClient.UDPStatusCallback() {

        @Override
        public void connected(int port,String ip) {
            Logger.i("udp connected port:ip="+port+":"+ip);
            /*
		     *Send local address and port to tcpService.
		     *Data format: "port:ip"
		     */

            TlvModes tlvModes=new TlvModes();
            tlvModes.setTlvMode(new TlvIntegerMode(TlvMode.T_PORT,port));
            tlvModes.setTlvMode(new TlvStringMode(TlvMode.T_IP,ip));

            PacketCmdHandler handler=new TcpInitCmdHandler();
            handler.setCommandResponseCallBack(tcpInitCallback);
            handler.setMessage(TlvContentFactory.createTlvContent(tlvModes));
            handler.setTcpClient(tcpClient);
            if(commandManager!=null){
                commandManager.addCommand(handler,true);
            }

        }

        @Override
        public void receive(byte[] packet) {
            /*
		     *Add udp byte[] into UdpFrameReceiver cache
		     */
            udpFrameReceiver.addUdpPacket(packet);
        }

    };


    public void setStatisticsListener(StatisticsProcessor.StatisticsListener statisticsListener) {
        if(udpFrameReceiver !=null){
            udpFrameReceiver.setStatisticsListener(statisticsListener);
        }
    }


    @Override
    public Void obtainStream(StreamCmdHandlerCallback callback) {
        if(udpFrameReceiver !=null){
            udpFrameReceiver.startRun();
        }

        StreamCmdHandler mc=new StreamCmdHandler();
        mc.setTcpClient(tcpClient);
        mc.setCommandType(TcpCommandType.COMMAND_START_STREAM);
        mc.setCommandResponseCallBack(callback);
        commandManager.addCommand(mc,true);
        udpPacketSender.startRun();
        return null;
    }

    @Override
    public Void releaseStream(ICmdHandlerCallback callback) {
        if(udpFrameReceiver !=null){
            udpFrameReceiver.stopRun();
        }
        udpPacketSender.stopRun();
        PacketCmdHandler mc=new StreamCmdHandler();
        mc.setTcpClient(tcpClient);
        mc.setCommandType(TcpCommandType.COMMAND_STOP_STREAM);
        commandManager.getTcpReceiver()
                .remPacketCmdHandler(TcpCommandType.COMMAND_START_STREAM);
        mc.setCommandResponseCallBack(callback);
        commandManager.addCommand(mc,true);

        return null;
    }

    @Override
    public void destroy() {
        if(commandManager!=null) {
            commandManager.stopRun();
        }

        //release tcp and udp
        releaseTcp();
        releaseUdp();
        if(udpFrameReceiver!=null){
            udpFrameReceiver.release();
        }
        super.destroy();
    }


}
