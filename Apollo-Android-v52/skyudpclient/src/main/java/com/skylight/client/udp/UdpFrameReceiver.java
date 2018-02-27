package com.skylight.client.udp;

import android.util.Log;
import com.skylight.client.udp.mode.PacketUdpBean;
import com.skylight.mode.WifiCameraMode;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Description: Udp packet receiver
 * Author: Created by lixby on 17-12-19.
 */

public class UdpFrameReceiver implements Runnable{

    private static final String TAG="FrameReceiver";

    //Cache the stream data
    private ConcurrentLinkedQueue<PacketUdpBean> udpPacketCache;

    private ExecutorService service;
    private boolean isRunning=false;
    private Future future;

    /**Udp packet processor*/
    private PacketProcessor packetProcessor;
    private StatisticsProcessor statisticsProcessor;

    public UdpFrameReceiver(WifiCameraMode wifiMode){
        udpPacketCache=new ConcurrentLinkedQueue();
        packetProcessor=new PacketProcessor(wifiMode);
        this.statisticsProcessor=new StatisticsProcessor();

    }

    /**Start operation*/
    public void startRun(){
        if(isRunning){
            return;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }
        isRunning=true;
        service= Executors.newCachedThreadPool();
        udpPacketCache.clear();
        future=service.submit(this);
        packetProcessor.startRun();
        statisticsProcessor.startRun();

    }

    /**Stop operation*/
    public void stopRun(){
        isRunning=false;
        if(future!=null){
            future.cancel(true);
            future=null;
        }

        if(service!=null&&!service.isShutdown()){
            service.shutdownNow();
            service=null;
        }

        udpPacketCache.clear();
        packetProcessor.stopRun();
        statisticsProcessor.stopRun();

    }

    public void release(){
        stopRun();
        packetProcessor.release();
        packetProcessor=null;
    }

    long a=0;

    public void addUdpPacket(byte[] data){
        if(isRunning){
            PacketUdpBean packetUdpBean=new PacketUdpBean();
            if(packetUdpBean.parsePacket(data)){
                Log.i(TAG,"addUdpPacket --"+packetUdpBean.getPacketIndex()+"--|QSize="+udpPacketCache.size()+"--|waitTime="+(System.currentTimeMillis()-a));
                statisticsProcessor.addPacket(packetUdpBean);
                udpPacketCache.add(packetUdpBean);
            }
            a=System.currentTimeMillis();
        }

        /*if(isRunning){
            PacketUdpBean packetUdpBean=new PacketUdpBean();
            if(packetUdpBean.parsePacket(data)){
                long a=System.currentTimeMillis();
                Log.i(TAG,"addUdpPacket --"+packetUdpBean.getPacketIndex()+"--|QSize="+udpPacketCache.size());

                try {
                    statisticsProcessor.addPacket(packetUdpBean);
                    udpPacketCache.put(packetUdpBean);

                    Log.i(TAG,"addUdpPacket -a-"+(System.currentTimeMillis()-a));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }*/
    }


    /**input stream packet*/
    @Override
    public void run() {
        while (isRunning){
            if(udpPacketCache.isEmpty()){
                continue;
            }

            PacketUdpBean udp_Packet = udpPacketCache.poll();
            if(packetProcessor!=null&&udp_Packet!=null){
                long a=System.currentTimeMillis();
                packetProcessor.receivePacket(udp_Packet);
                Log.i(TAG,"take-----取数据=packetIndex"+(System.currentTimeMillis()-a));
            }
        }

    }


    public void setStatisticsListener(StatisticsProcessor.StatisticsListener statisticsListener) {
        this.statisticsProcessor.setStatisticsListener(statisticsListener);
    }



}
