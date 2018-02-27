package com.skylight.client.udp;

import android.os.SystemClock;
import android.util.Log;

import com.skylight.client.udp.mode.PacketUdpBean;
import com.skylight.client.udp.mode.StatisticsBean;
import com.skylight.util.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Description:
 * Author: Created by lixby on 18-1-11.
 */

public class StatisticsProcessor implements  Runnable{


    private LinkedBlockingQueue<PacketUdpBean> packetCache=new LinkedBlockingQueue<>();
    private ScheduledExecutorService service;
    private ExecutorService executorService;

    private LossRateRunnable lossRateRunnable;
    private ScheduledFuture scheduledFuture;
    private Future future;

    private boolean isRunning=false;

    /**丢包统计计算间隔时间，单位：毫秒*/
    private static final long CAlC_INTERVAL=1000;

    /**收到包的最大序号*/
    private long packet_MaxNum=-1;
    /**收到包的最小序号*/
    private long packet_MinNum=0;

    /**当前收到包数*/
    private long cur_Packets=0;
    /**预期收到的包数*/
    private long exd_Packets=0;

    private long lastStatisticTime=0;
    private int dataLen=0;


    public StatisticsProcessor(){
        lossRateRunnable=new LossRateRunnable();
    }

    public void addPacket(PacketUdpBean bean){
        try {
            packetCache.put(bean);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public void startRun(){
        if(isRunning){
            return;
        }
        isRunning=true;
        packetCache.clear();
        executorService=Executors.newCachedThreadPool();
        service= Executors.newScheduledThreadPool(1);

        future=executorService.submit(this);
        scheduledFuture=service.scheduleWithFixedDelay(lossRateRunnable ,
                CAlC_INTERVAL , CAlC_INTERVAL , TimeUnit.MILLISECONDS);

    }

    public void stopRun(){
        isRunning=true;
        if(future!=null){
            future.cancel(true);
        }

        if(scheduledFuture!=null){
            scheduledFuture.cancel(true);
        }

        if(service!=null){
            service.shutdownNow();
            service=null;
        }

        if(executorService!=null){
            executorService.shutdownNow();
            executorService=null;
        }

        clear();
    }

    private void clear(){
        packet_MaxNum=-1;
        packet_MinNum=0;
        cur_Packets=0;
        exd_Packets=0;
    }

    public synchronized void statisticsPacket(PacketUdpBean packet){
        long packetNum=packet.getPacketIndex();
        //Logger.w("calcLossRate packetNum= "+packetNum);
        if(packetNum>packet_MaxNum){
            packet_MaxNum=packetNum;
            cur_Packets++;
            //Logger.w("calcLossRate cur_Packets= "+cur_Packets);
        }

        if(lastStatisticTime==0){
            lastStatisticTime=System.currentTimeMillis();
            //Logger.i("calcLossRate lastStatisticTime= "+lastStatisticTime);
        }

        dataLen+=packet.getPacketLen()+18;
    }

    private synchronized void calcLossRate(){
        if(packet_MaxNum==-1){
            return;
        }

        //Calculate bandwidth per second
        float time_Interval= (float) (((System.currentTimeMillis()-lastStatisticTime)*1.0)/1000);
        BigDecimal a = new BigDecimal((dataLen/time_Interval)/1024);
        float bandwidth = a.setScale(3, BigDecimal.ROUND_HALF_UP).floatValue();
        //Logger.w("calcLossRate time_Interval= "+time_Interval+"|data="+dataLen+"byte"+"|-bd="+bandwidth+"KB/s");

        //Calculate the packet loss rate
        exd_Packets=(packet_MaxNum-packet_MinNum)+1;
        long lossPackets=exd_Packets-cur_Packets;
        BigDecimal b = new BigDecimal((lossPackets*1.0/exd_Packets)*100);
        float lossRate = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();

        if(statisticsListener!=null){
            statisticsListener.statisticalResult(lossRate,bandwidth);
        }

        lastStatisticTime=0;
        dataLen=0;
        //Logger.w("calcLossRate exd_Packets= "+exd_Packets+" |loss_Packets="+lossPackets+"|lossRote: "+lossRate+"%|bandwidth="+bandwidth);

    }

    @Override
    public void run() {
        while (isRunning){
            try {
                PacketUdpBean packet=packetCache.take();
                if(packet!=null){
                    statisticsPacket(packet);
                }
            } catch (InterruptedException e) {

            }
        }
        
    }


    private class LossRateRunnable implements Runnable{

        @Override
        public void run() {
            calcLossRate();
        }

    }

    private  StatisticsListener statisticsListener;

    public void setStatisticsListener(StatisticsListener statisticsListener) {
        this.statisticsListener = statisticsListener;
    }

    public interface StatisticsListener{
        void statisticalResult(float lossRate, float bandwidth);
    }



}
