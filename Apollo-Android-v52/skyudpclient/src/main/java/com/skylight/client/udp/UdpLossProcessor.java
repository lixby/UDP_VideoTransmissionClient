package com.skylight.client.udp;

import com.skylight.client.tcp.handler.UdpPacketSAKHandler;
import com.skylight.client.tcp.mode.TlvSAKMode;
import com.skylight.util.CmLog;
import java.util.ArrayList;

/**
 * Description: Udp packet loss processor
 * Author: Created by lixby on 17-12-28.
 */

public class UdpLossProcessor {


    /**Packet loss number cache*/
    private ArrayList<Long> lostCache;

    /**The maximum number of packets received*/
    public long maxSerialNum_Rec=-1;
    /**The minimum number of packets received*/
    public long minSerialNum_Rec=-1;

    /**Drop packet queue status*/
    private boolean lostChanged=false;

    private boolean isRunning=false;

    private long minPocketEdgeNum=0;

    public UdpLossProcessor(){
        lostCache=new ArrayList<>();
    }

    /**Start status*/
    public void startRun(){
        isRunning=true;

    }

    /**Calculate packet loss*/
    public void calculatePacketLoss(long packetNum){
        if(!isRunning){
            return;
        }

        if(packetNum>maxSerialNum_Rec){
            calculateLoss(packetNum);
            maxSerialNum_Rec=packetNum;
        }

        if(minSerialNum_Rec==-1){
            minSerialNum_Rec=packetNum;
        }else if(packetNum<minSerialNum_Rec){
            minSerialNum_Rec=packetNum;
        }

        deleteLost(packetNum);
        createSakResponse(minPocketEdgeNum,packetNum,true);
    }

    /**Calculate and add packet loss*/
    private void calculateLoss(long packetNum){
        long space=packetNum-maxSerialNum_Rec;
        if(space>1 && isRunning){
            for (int i = 1; i <space && isRunning ; i++) {
                long seqNum=maxSerialNum_Rec+i;
                addPacketLoss(seqNum);
            }
        }

    }

    /**Add the lost packet number in the cache queue*/
    private void addPacketLoss(long packetNum){
        if(isRunning&&!lostCache.contains(packetNum)){
            CmLog.d("add lost packetIndex="+packetNum);
            lostCache.add(packetNum);
            lostChanged=true;
        }

    }

    /**Delete the lost packet number in the cache queue*/
    public void deleteLost(long packetNum){
        if(isRunning&&lostCache.contains(packetNum)){
            lostCache.remove(packetNum);
            lostChanged=true;
        }

    }

    /**Update the window boundary value*/
    public void updateEdge(long minPocketEdgeNum){
        this.minPocketEdgeNum=minPocketEdgeNum;
        if(isRunning&&lostCache.size()>0){
            for (int i = 0; i < lostCache.size(); i++) {
                long cacheNum=lostCache.get(i);
                if(cacheNum<minPocketEdgeNum){
                    lostCache.remove(cacheNum);
                }
            }
        }

        createSakResponse(minPocketEdgeNum,-1,false);
    }


    private void createSakResponse(long minPocketEdgeNum, long packetNum,boolean syncLost){
        if(isRunning&&lostChanged){
            Long[] lostNums = null;
            if(syncLost){
                if(lostCache.size()>0){
                    lostNums=new Long[lostCache.size()];
                    lostCache.toArray(lostNums);
                }
            }

            TlvSAKMode tlvSak=new TlvSAKMode(minPocketEdgeNum,packetNum,lostNums);
            UdpPacketSAKHandler sakHandler=new UdpPacketSAKHandler(tlvSak);
            if(sakResponseListener!=null&&isRunning){
                sakResponseListener.callBackSAKHandler(sakHandler);
            }

        }

    }

    /**Stop status*/
    public void stopRun(){
        isRunning=false;
        lostChanged=false;
        maxSerialNum_Rec=-1;
        minSerialNum_Rec=-1;
        minPocketEdgeNum=0;
        lostCache.clear();

    }

    private SAkResponseListener sakResponseListener;

    public void setSakResponseListener(SAkResponseListener sakResponseListener) {
        this.sakResponseListener = sakResponseListener;
    }

    public  interface  SAkResponseListener{
        void callBackSAKHandler(UdpPacketSAKHandler sak);

    }


}
