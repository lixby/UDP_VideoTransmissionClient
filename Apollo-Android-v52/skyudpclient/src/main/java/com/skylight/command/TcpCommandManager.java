package com.skylight.command;

import android.util.Log;
import com.skylight.client.tcp.TcpPacketReceiver;
import com.skylight.util.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Description: TcpCommandManager for send command to tcp service
 * Author: Created by lixby on 17-12-18.
 *
 */

public class TcpCommandManager implements Runnable{

    //Message command queue
    private LinkedBlockingQueue<PacketCmdHandler> cmdQueue = new LinkedBlockingQueue();
    private ExecutorService service;
    private Future future;
    private boolean executing=false;

    private TcpPacketReceiver tcpPacketReceiver;

    public TcpCommandManager(){
        //Init tcp packet receiver
        tcpPacketReceiver=new TcpPacketReceiver();
    }

    public TcpPacketReceiver getTcpReceiver() {
        return tcpPacketReceiver;
    }


    public void startRun(){
        if(service==null){
            service= Executors.newCachedThreadPool();
        }

        if(!executing){
            executing=true;
            future=service.submit(this);
            startTcpRec();
        }

    }

    private void startTcpRec(){
        if(tcpPacketReceiver!=null){
            tcpPacketReceiver.startRun();
        }
    }

    private void stopTcpRec(){
        if(tcpPacketReceiver!=null){
            tcpPacketReceiver.stopRun();
            tcpPacketReceiver=null;
        }
    }


    public void addCommand(PacketCmdHandler handler){
        try {
            cmdQueue.put(handler);
            Logger.d("addTcp-Command---size="+cmdQueue.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param handler PacketCmdHandler
     * @param isResponse  Need to respond to Tcp requests ,True :response,False:no
     */
    public void addCommand(PacketCmdHandler handler,boolean isResponse){
        addCommand(handler);
        if(isResponse&&tcpPacketReceiver!=null){
            tcpPacketReceiver.addSendHandler(handler);
        }

    }

    public void stopRun(){
        executing=false;
        if(future!=null){
            future.cancel(true);
            future=null;
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }
        cmdQueue.clear();
        stopTcpRec();

    }

    @Override
    public void run() {
        while (executing){
            try {
                PacketCmdHandler packetHandler = cmdQueue.take();
                if(packetHandler!=null){
                    if(packetHandler.getStatus()!=CommandHandler.CMD_STOP_EXECUTE){
                        Logger.d("cmdQueue---size="+cmdQueue.size());
                        packetHandler.execute();
                    }else{
                        if(tcpPacketReceiver!=null){
                            tcpPacketReceiver.remPacketCmdHandler(packetHandler);
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

    }




}
