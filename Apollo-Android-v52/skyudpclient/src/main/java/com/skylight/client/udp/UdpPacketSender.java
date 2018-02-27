package com.skylight.client.udp;

import com.skylight.client.tcp.handler.UdpPacketSAKHandler;
import com.skylight.util.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Description: UDP data sender for send UDP SAK packet
 * Author: Created by lixby on 18-1-19.
 */

public class UdpPacketSender implements Runnable{

    //Message command queue
    private ConcurrentLinkedQueue<UdpPacketSAKHandler> cmdQueue = new ConcurrentLinkedQueue();
    private ExecutorService service;
    private Future future;
    private boolean isRunning=false;

    private UdpStreamClient udpStreamClient;

    public UdpPacketSender() {

    }

    public UdpPacketSender(UdpStreamClient udpStreamClient){
        this.udpStreamClient=udpStreamClient;
    }

    public void startRun(){
        if(isRunning){
            return;
        }
        isRunning=true;
        if(service==null){
            service= Executors.newCachedThreadPool();
        }
        future=service.submit(this);
    }

    public void addUDPSAK(UdpPacketSAKHandler handler){
        if(isRunning){
            cmdQueue.add(handler);
            Logger.d("addUDP-Command---size="+cmdQueue.size());
        }

    }

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
        cmdQueue.clear();
    }

    @Override
    public void run() {
        while (isRunning){
            UdpPacketSAKHandler packetHandler = cmdQueue.poll();
            if(packetHandler!=null&&udpStreamClient!=null){
                udpStreamClient.sendPacket(packetHandler.createSAK());
            }
        }
    }

}
