package com.skylight.client.udp;

import android.util.Log;
import com.skylight.client.udp.mode.FrameBean;
import com.skylight.client.udp.mode.PacketUdpBean;
import com.skylight.util.Logger;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Description: This is used to process group packets, packet loss,
 * and synchronization status of Frames to the server.
 *
 * Author: Created by lixby on 17-12-28.
 */

public class FrameFactory {

    private static final String TAG="FrameFactory";

    //Cache frame data
    private TreeMap<Integer,FrameBean> framesCache=new TreeMap<>();

    private ConcurrentLinkedQueue<FrameBean> outFrameCache;

    private ScheduledExecutorService executorService;
    private ScheduledFuture timerFuture;

    /**Timed task execution interval,Unit：ms*/
    private static final int INTERVAL=3;

    /**Current point to the flow packet window number*/
    private long minPocketEdgeNum=0;

    /**The largest frame number*/
    private static final int MAX_FRAME_NUM=65534;
    /**Current head frame number*/
    private int curHeadFrameNum=-1;

    private boolean isRunTimer=false;

    /**在发出第一个I帧之后才能发送后面的P帧*/
    private boolean isOutIFrame=false;
    /**在发出视频帧之后才能发送后面的音频帧*/
    private boolean isOutVideoFrame=false;

    private Object syncLock;

    /**Head frame*/
    private FrameBean headFrame=null;

    public FrameFactory(ConcurrentLinkedQueue<FrameBean> outFrameCache,Object syncLock) {
        this.outFrameCache=outFrameCache;
        this.syncLock=syncLock;
    }

    public void startRun(){
        if(isRunTimer){
            return;
        }
        release();
        startExecuteTimer();

    }

    public void stopRun(){
        if(!isRunTimer){
            return;
        }
        stopExecuteTimer();
        release();

    }


    /**Start timer*/
    private void startExecuteTimer(){
        if(executorService!=null){
            executorService.shutdownNow();
            executorService=null;
        }

        executorService= Executors.newScheduledThreadPool(1);
        timerFuture=executorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"------------------------------------------scheduleAtFixedRate-try-lock--0000");
                synchronized (syncLock){
                    long a=System.currentTimeMillis();
                    checkHeadFrameState();
                    updateFramePeriod();
                    Log.i("lixby","*******************scheduleAtFixedRate-end"+(System.currentTimeMillis()-a));
                }
            }
        },INTERVAL,INTERVAL,TimeUnit.MILLISECONDS);
        isRunTimer=true;

    }

    /**Stop timer*/
    private void stopExecuteTimer(){
        if(executorService!=null){
            isRunTimer=false;
            executorService.shutdownNow();
            if(timerFuture!=null){
                timerFuture.cancel(true);
                timerFuture=null;
            }
            executorService=null;
        }

    }

    /**
     * Add udp data to the queue
     * @param packetUdpBean
     */
    public  void addToFrameBean(PacketUdpBean packetUdpBean) {
        long packetIndex=packetUdpBean.getPacketIndex();
        Log.w(TAG,"addToFrameBean minPocketEdgeNum="+minPocketEdgeNum+"|-packetIndex="+packetIndex+"|-PackageCount"+packetUdpBean.getPackageCount()+"|-FrameIndex="+packetUdpBean.getFrameIndex());
        if(packetIndex>=getMinPocketEdgeNum()){
            int frameIndex=packetUdpBean.getFrameIndex();
            if(isContains(frameIndex)){
                FrameBean frameBean=getFrameBean(frameIndex);
                if(frameBean!=null){
                    frameBean.addNewPacket(packetUdpBean);
                }
            }else{
                Log.i(TAG,"addToFrameBean -new-FrameBean-"+frameIndex);
                FrameBean frameBean=new FrameBean();
                frameBean.addNewPacket(packetUdpBean);
                addNewFrameBean(frameBean.getFrameIndex(),frameBean);
            }
        }

    }

    private void addNewFrameBean(int key,FrameBean frameBean){
        framesCache.put(key,frameBean);
        if(headFrame==null||key<curHeadFrameNum){
            updateHeadFrame();
        }

    }

    private boolean isContains(int key){
        return framesCache.containsKey(key);
    }

    private FrameBean getFrameBean(int key){
        return framesCache.get(key);
    }

    private void removeFrameBean(int key){
        if(isContains(key)){
            framesCache.remove(key);
        }
    }

    public long getMinPocketEdgeNum() {
        return minPocketEdgeNum;
    }

    /**Update head frame*/
    private void updateHeadFrame() {
        if(framesCache.size()>0&&isRunTimer){
            curHeadFrameNum=framesCache.firstKey();
            headFrame=getFrameBean(curHeadFrameNum);
            Log.d(TAG,"curHeadFrameNum="+curHeadFrameNum+"-|--headFrame ="+headFrame);
        }else{
            headFrame=null;
            curHeadFrameNum=-1;
            Log.e(TAG,"curHeadFrameNum=-1");
        }

    }

    /**Check head frame state*/
    private void checkHeadFrameState(){
        if(framesCache.size()==0&&!isRunTimer){
            return;
        }

        if(headFrame==null){
            updateHeadFrame();
        }

        if(headFrame!=null){
            if(headFrame.getFrameType()==FrameBean.FRAME_AUDIO){
                if(isOutVideoFrame){
                    sendFrame();
                }else{
                    Log.i(TAG,"FRAME_AUDIO--delete frame");
                    deleteFrame();
                }

            }else{
                if(headFrame.getIbpType()==FrameBean.FRAME_I){
                    checkFrameState_I();
                }else{
                    if(headFrame.getIbpType()==FrameBean.FRAME_P){
                        checkFrameState_P();
                    }else{
                        Log.i("lixby","checkHeadFrameState--deleteFrame");
                        deleteFrame();
                    }
                }
            }
        }

    }

    private void checkFrameState_I(){
        if(headFrame.getFrameState()==FrameBean.FRAME_STATE_COMPLETE){
            isOutIFrame=true;
            Log.i(TAG,"checkFrameState_I--sendFrame");
            sendFrame();

        }else if(headFrame.getFrameState()==FrameBean.FRAME_STATE_DISCARD){
            Log.i(TAG,"checkFrameState_I--delete frame");
            isOutIFrame=false;
            deleteFrame();
        }

    }

    private void checkFrameState_P(){
        if(isOutIFrame){
            if(headFrame.getFrameState()==FrameBean.FRAME_STATE_COMPLETE){
                Log.i(TAG,"checkFrameState_P--send frame");
                sendFrame();

            }else if(headFrame.getFrameState()==FrameBean.FRAME_STATE_DISCARD){
                Log.i(TAG,"checkFrameState_P-0-delete frame");
                deleteFrame();
            }

        }else{
            Log.i(TAG,"checkFrameState_P-1-delete frame");
            deleteFrame();
        }

    }


    /**Check and update the status of all FrameBean*/
    private void updateFramePeriod(){
        if(framesCache.size()==0||!isRunTimer){
            return;
        }

        Iterator<Integer> frameInteger=framesCache.keySet().iterator();
        while (frameInteger.hasNext()&&isRunTimer){
            int frameIndex=frameInteger.next();
            FrameBean frame=framesCache.get(frameIndex);
            frame.updatePeriod();
        }

    }


    /**Send a complete package of frames*/
    private void sendFrame(){
        try {
            long endPacketNum=headFrame.getEndPacketNum();
            if(endPacketNum==FrameBean.MAX_PACKET_NUM){
                minPocketEdgeNum=0;
            }else{
                minPocketEdgeNum=endPacketNum+1;
            }

            //Add frame to queue
            if(this.outFrameCache!=null){
                Log.w(TAG,"OutPutCompleteFrame-add-frame");
                this.outFrameCache.add(headFrame);
                isOutVideoFrame=true;
            }

            //delete head frame
            removeFrameBean(curHeadFrameNum);
            headFrame=null;

            if(packetStatusListener!=null){
                packetStatusListener.edgeNumChanged(minPocketEdgeNum);
            }

            //Get next head frame
            updateHeadFrame();
            Log.i(TAG,"sendFrame frame minPocketEdgeNum ="+minPocketEdgeNum +"|--curHeadFrameNum="+curHeadFrameNum);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**Delete discarded frame data*/
    private void deleteFrame(){
        long endPacketNum=headFrame.getEndPacketNum();
        if(endPacketNum==FrameBean.MAX_PACKET_NUM){
            minPocketEdgeNum=0;
        }else{
            minPocketEdgeNum=endPacketNum+1;
        }

        //delete head frame
        removeFrameBean(curHeadFrameNum);
        headFrame=null;

        if(packetStatusListener!=null){
            packetStatusListener.edgeNumChanged(minPocketEdgeNum);
        }

        //Get next head frame
        updateHeadFrame();

        Log.e(TAG,"delete frame minPocketEdgeNum ="+minPocketEdgeNum +"|--curHeadFrameNum="+curHeadFrameNum);

    }

    private void release(){
        framesCache.clear();
        headFrame=null;
        curHeadFrameNum=0;
        minPocketEdgeNum=0;

    }


    private PacketStatusListener packetStatusListener;

    public void setPacketStatusListener(PacketStatusListener packetStatusListener) {
        this.packetStatusListener = packetStatusListener;
    }

    public interface PacketStatusListener{
        void edgeNumChanged(long minPocketEdgeNum);

    }



}
