package com.skylight.client.tcp;

import android.text.TextUtils;
import com.skylight.client.tcp.handlercallback.StreamCmdHandlerCallback;
import com.skylight.client.tcp.handlercallback.TcpIpStatusHandlerCallback;
import com.skylight.client.tcp.mode.TcpIpInformation;
import com.skylight.client.tcp.mode.TlvMode;
import com.skylight.client.tcp.mode.PackageBean;
import com.skylight.command.PacketCmdHandler;
import com.skylight.command.callback.CmdStatus;
import com.skylight.command.callback.ICmdHandlerCallback;
import com.skylight.util.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Description:
 * Author: Created by lixby on 17-12-19.
 */

public class TcpPacketReceiver implements Runnable{

    //Cache necessary send PacketHandler for callback to application
    private Map<Integer,PacketCmdHandler> sendHandlerCache=new ConcurrentHashMap<>();
    //Cache necessary receive byte[]
    private LinkedBlockingQueue<byte[]> receiveHandlerCache=new LinkedBlockingQueue<>();

    private StreamCmdHandlerCallback streamCallback;

    private ExecutorService service;
    private boolean isRunning=false;
    private Future future;

    private TcpStickyProcessor stickyProcessor;

    public TcpPacketReceiver(){
        stickyProcessor=new TcpStickyProcessor();
        stickyProcessor.setSplitPacketListener(splitPacketListener);
    }

    public void startRun(){
        if(isRunning){
            return;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }
        isRunning=true;
        service=Executors.newCachedThreadPool();
        future=service.submit(this);

    }

    public void stopRun(){
        isRunning=false;
        if(future!=null){
            future.cancel(true);
            future=null;
        }

        if(!service.isShutdown()){
            service.shutdownNow();
            service=null;
        }

        sendHandlerCache.clear();
        receiveHandlerCache.clear();
    }

    /**
     * Add PacketCmdHandler
     * @param handler
     */
    public  void addSendHandler(PacketCmdHandler handler){
        sendHandlerCache.put(handler.getCommandType(),handler);
        if(handler.getCommandType()==TcpCommandType.COMMAND_START_STREAM){
            this.streamCallback= (StreamCmdHandlerCallback) handler.getCmdHandlerCallback();
        }

    }

    /**
     * Remove PacketCmdHandler
     * @param handler
     */
    public  void remPacketCmdHandler(PacketCmdHandler handler){
        if(sendHandlerCache.containsKey(handler.getCommandType())){
            if(handler.getCommandType()==TcpCommandType.COMMAND_START_STREAM){
                this.streamCallback=null;
            }
            sendHandlerCache.remove(handler.getCommandType());
        }

    }

    /**
     * Remove handler by command type.
     * @param cmdType
     */
    public  void remPacketCmdHandler(int cmdType){
        if(sendHandlerCache.containsKey(cmdType)){
            sendHandlerCache.remove(cmdType);
        }
        if(cmdType==TcpCommandType.COMMAND_START_STREAM){
            this.streamCallback=null;
        }

    }

    /**
     * Get handler by command type.
     * @param cmdType
     * @return
     */
    public PacketCmdHandler getPacketCmdHandler(int cmdType){
        PacketCmdHandler cmdHandler = null;
        if(sendHandlerCache.containsKey(cmdType)){
            cmdHandler=sendHandlerCache.get(cmdType);
        }

        return cmdHandler;
    }

    public StreamCmdHandlerCallback getStreamCallback() {
        return streamCallback;
    }

    /**
     * Add tcp packet to the queue
     * @param packet
     */
    public  void addRecPacket(byte[] packet){
        try {
            receiveHandlerCache.put(packet);
        } catch (InterruptedException e) {}

    }

    @Override
    public void run() {
        while (isRunning){
            try {
                byte[] packet=receiveHandlerCache.take();
                if(packet!=null){
                    stickyProcessor.splitTcpPacket(packet);
                }

            } catch (InterruptedException e) {}
        }

    }


    TcpStickyProcessor.SplitPacketListener splitPacketListener=new TcpStickyProcessor.SplitPacketListener() {

        @Override
        public void splitCompleted(PackageBean packageBean) {
            Logger.i("splitCompleted-commandType="+packageBean.commandType);
            if(packageBean!=null&&packageBean.tlvCache.size()>0){
                switch(packageBean.commandType){
                    case TcpCommandType.COMMAND_UDP_IPPORT: //Parse server UDP ip and port
                        String sUDP_IP="";
                        int sUDP_port=-1;
                        if(packageBean.tlvCache.size()>0){
                            for (int i = 0; i <packageBean.tlvCache.size()&&isRunning ; i++) {
                                TlvMode tlvMode=packageBean.tlvCache.get(i);
                                if(tlvMode!=null){
                                    if(tlvMode.getType()==TlvMode.T_PORT){
                                        sUDP_port= (int) parseTlvData(tlvMode,TlvMode.DATA_INT,TlvMode.V_SERUDP_PORT_LEN);
                                        Logger.d("sUDP_port="+sUDP_port);
                                    }

                                    if(tlvMode.getType()==TlvMode.T_IP){
                                        try {
                                            sUDP_IP = InetAddress.getByAddress(tlvMode.getTlvData()).toString();
                                            sUDP_IP=sUDP_IP.substring(1,sUDP_IP.length());
                                            Logger.d("sUDP_IP="+sUDP_IP);
                                        } catch (UnknownHostException e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }
                            }
                        }

                        if(sUDP_port!=-1&& !TextUtils.isEmpty(sUDP_IP)&&isRunning){
                            TcpIpInformation.getInstance().setServerUdp_IP(sUDP_IP);
                            TcpIpInformation.getInstance().setServerUdp_Port(sUDP_port);
                            if(sendHandlerCache.containsKey(packageBean.commandType)&&isRunning){
                                PacketCmdHandler cmdHandler=sendHandlerCache.get(packageBean.commandType);
                                TcpIpStatusHandlerCallback callback= (TcpIpStatusHandlerCallback) cmdHandler.getCmdHandlerCallback();
                                callback.inItSuccess();
                                sendHandlerCache.remove(packageBean.commandType);
                            }

                        }
                        break;
                    case TcpCommandType.COMMAND_START_STREAM: //Start stream
                    case TcpCommandType.COMMAND_STOP_STREAM:  //Stop stream
                        parseTlvResponse(packageBean);
                        break;
                    default:
                        break;
                }
            }
        }
    };

    private void parseTlvResponse(PackageBean packageBean){
        CmdStatus cmdStatus=parseCmdStatus(packageBean);
        if(sendHandlerCache.containsKey(packageBean.commandType)&&isRunning){
            PacketCmdHandler cmdHandler=sendHandlerCache.get(packageBean.commandType);
            ICmdHandlerCallback callback= cmdHandler.getCmdHandlerCallback();
            if(callback!=null&&isRunning){
                callback.responseStatus(cmdStatus);
                sendHandlerCache.remove(packageBean.commandType);
            }
        }
    }

    private CmdStatus parseCmdStatus(PackageBean packageBean){
        CmdStatus cmdStatus=null;
        if(packageBean.tlvCache.size()>0){
            for (int i = 0; i <packageBean.tlvCache.size()&&isRunning ; i++) {
                TlvMode tlvMode=packageBean.tlvCache.get(i);
                if(tlvMode!=null){
                    if(tlvMode.getType()==TlvMode.T_RESPONSE_OK||tlvMode.getType()==TlvMode.T_RESPONSE_ERROR){
                        byte[] tlvData=tlvMode.getTlvData();
                        //state code(1byte)
                        int stateCode=tlvData[0];
                        //其他是携带结果消息
                        int daLen=tlvData.length-TlvMode.V_RESCODE_LEN;

                        byte[] re=new byte[daLen];
                        System.arraycopy(tlvData,TlvMode.V_RESCODE_LEN,re,0,daLen);
                        String message=new String(tlvData);
                        cmdStatus.setCode(stateCode);
                        cmdStatus.setMessage(message);

                    }
                }
            }
        }

        return cmdStatus;
    }

    private Object parseTlvData(TlvMode tlvMode,int type,int byteSize){
        if(type==TlvMode.DATA_TEXT){
            String stResult=new String(tlvMode.getTlvData());
            return  stResult;
        }else{
            return tlvMode.bytes2Int(tlvMode.getTlvData(),0,byteSize);
        }

    }




}
