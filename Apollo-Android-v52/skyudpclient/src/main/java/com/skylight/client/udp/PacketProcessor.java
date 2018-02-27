package com.skylight.client.udp;

import android.util.Log;
import com.skylight.client.tcp.TcpPacketReceiver;
import com.skylight.client.tcp.handler.UdpPacketSAKHandler;
import com.skylight.client.tcp.handlercallback.StreamCmdHandlerCallback;
import com.skylight.client.udp.mode.FrameBean;
import com.skylight.client.udp.mode.PacketUdpBean;
import com.skylight.mode.WifiCameraMode;
import com.skylight.util.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Description: Udp packet processor
 * Author: Created by lixby on 18-1-6.
 */

public class PacketProcessor {

    /**Cache the complete output frame data*/
    private ConcurrentLinkedQueue<FrameBean> outFrameCache=new ConcurrentLinkedQueue<>();

    private FrameFactory frameFactory;
    private PacketLossProcessor lossProcessor;
    private WifiCameraMode wifiMode;

    private OutPutCompleteFrame completeFrame;
    private boolean isRunning=false;

    private ExecutorService service;
    private Future future;

    private Object syncLock=new Object();

    public PacketProcessor(WifiCameraMode wifiMode){
        this.wifiMode=wifiMode;
        this.frameFactory=new FrameFactory(outFrameCache,syncLock);
        this.lossProcessor=new PacketLossProcessor();
        this.completeFrame=new OutPutCompleteFrame();

    }

    /**Start run*/
    public void startRun(){
        if(isRunning){
            return;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }

        isRunning=true;
        outFrameCache.clear();
        service= Executors.newCachedThreadPool();
        future=service.submit(completeFrame);

        frameFactory.startRun();
        frameFactory.setPacketStatusListener(packetStatusListener);
        lossProcessor.startRun();
        lossProcessor.setSakResponseListener(sakResponseListener);

    }

    /**Stop run*/
    public void stopRun(){
        isRunning=false;
        if(future!=null){
            future.cancel(true);
            future=null;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }

        frameFactory.stopRun();
        frameFactory.setPacketStatusListener(null);
        lossProcessor.stopRun();
        lossProcessor.setSakResponseListener(null);
        outFrameCache.clear();

    }

    public void release(){
        this.wifiMode=null;
        this.frameFactory=null;
        this.lossProcessor=null;
    }


    /**Receive udp packet data*/
    public void receivePacket(PacketUdpBean packet){
        Log.i("lixby","------------------------------------receivePacket-try-lock");
        synchronized (syncLock){
            if(isRunning){
                Log.i("lixby","receivePacket -取出数据PacketIndex="+packet.getPacketIndex()+"|-MinPocketEdgeNum()="+frameFactory.getMinPocketEdgeNum());
                if(packet.getPacketIndex()>=frameFactory.getMinPocketEdgeNum()){
                    lossProcessor.calculatePacketLoss(packet.getPacketIndex());
                    frameFactory.addToFrameBean(packet);
                }else{
                    packet=null;
                }
            }
        }

    }


    FrameFactory.PacketStatusListener packetStatusListener=new FrameFactory.PacketStatusListener() {

        @Override
        public void edgeNumChanged(long minPocketEdgeNum) {
            if(lossProcessor!=null&&isRunning){
                lossProcessor.updateEdge(minPocketEdgeNum);
            }

        }
    };


    PacketLossProcessor.SAkResponseListener sakResponseListener=new PacketLossProcessor.SAkResponseListener() {

        @Override
        public void callBackSAKHandler(UdpPacketSAKHandler sak) {
            if(wifiMode!=null&&isRunning&&wifiMode.getUdpPacketSender()!=null){
                //wifiMode.getUdpPacketSender().addUDPSAK(sak);
                wifiMode.getUdpClient().sendPacket(sak.createSAK());
            }

        }
    };

    /**Output stream data*/
    private class  OutPutCompleteFrame implements Runnable{

        @Override
        public void run() {
            while (isRunning){
                if(outFrameCache.isEmpty()){
                    continue;
                }

                FrameBean frameBean = outFrameCache.poll();
                if(wifiMode!=null&&frameBean!=null){
                    Logger.i("OutPutCompleteFrame");
                    TcpPacketReceiver tcpReceiver=wifiMode.getCommandManager().getTcpReceiver();
                    if(tcpReceiver!=null){
                        StreamCmdHandlerCallback callback=tcpReceiver.getStreamCallback();
                        if(callback!=null){
                            byte[] outData=frameBean.startFraming();
                            callback.responseReadFrame(frameBean.getFrameType(),outData,outData.length,0);
                        }
                    }

                }

            }
        }

    }







}
