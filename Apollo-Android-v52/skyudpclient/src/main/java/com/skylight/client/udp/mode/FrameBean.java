package com.skylight.client.udp.mode;

import android.util.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Record each frame of audio and video data, complete the framing work.
 */

public class FrameBean {


	/**UDP Stream video frame*/
	public static final long FRAME_VIDEO=0X0a;
	/**UDP Stream audio frame*/
	public static final long FRAME_AUDIO=0X14;

	/**I frame*/
	public static final long FRAME_I=0X20;
	/**P frame*/
	public static final long FRAME_P=0X21;

	/**接收到第一包的状态，需要初始化数据*/
	public static final int FRAME_STATE_CREATE=1;
	/**等待接收数据状态*/
	public static final int FRAME_START_WAIT=2;
	/**接收数据完成状态*/
	public static final int FRAME_STATE_COMPLETE=3;
	/**丢弃帧状态*/
	public static final int FRAME_STATE_DISCARD=4;

	private int frameState=FRAME_STATE_CREATE;

	/**The largest package serial number*/
	public static final long MAX_PACKET_NUM=4294967295l;

	/**Tundish serial number*/
	private long middlePacketNum=-1;

	/**Receive a data for the longest time,Unit: millisecond*/
	private long period=1500;
	/**Detection cycle,Unit: millisecond*/
	private long checkCycle=3;

	/**帧索引,开始为0*/
	public int	frameIndex;
	/**帧类型（音频和视频）*/
	public int frameType;

	/**帧类型（I和P）*/
	private int ibpType;

	/**帧长（所有包的总长度，包增加的过程中一直在累加）*/
	public int frameLength=0;

	/**此帧数据的总包数*/
	public int	packetCount;

	/**当前链表中存放的包数*/
	public int	curPacketCount=0;
	/**该帧包的开始序号*/
	public long startPacketNum;
	/**该帧包的结束序号*/
	public long endPacketNum;

	public int getFrameIndex() {
		return frameIndex;
	}

	public void setFrameIndex(int frameIndex) {
		this.frameIndex = frameIndex;
	}

	public int getFrameType() {
		return frameType;
	}

	public void setFrameType(int frameType) {
		this.frameType = frameType;
	}

	public int getFrameLength() {
		return frameLength;
	}

	public void setFrameLength(int frameLength) {
		this.frameLength = frameLength;
	}

	public int getPacketCount() {
		return packetCount;
	}

	public void setPacketCount(int packetCount) {
		this.packetCount = packetCount;
	}

	public int getCurPacketCount() {
		return curPacketCount;
	}

	public void setCurPacketCount(int curPacketCount) {
		this.curPacketCount = curPacketCount;
	}

	public long getStartPacketNum() {
		return startPacketNum;
	}

	public void setStartPacketNum(long startPacketNum) {
		this.startPacketNum = startPacketNum;
	}

	public int getIbpType() {
		return ibpType;
	}

	public void setIbpType(int ibpType) {
		this.ibpType = ibpType;
	}

	public long getMiddlePacketNum() {
		return middlePacketNum;
	}

	public void setMiddlePacketNum(int middlePacketNum) {
		this.middlePacketNum = middlePacketNum;
	}

	public long getEndPacketNum() {
		return endPacketNum;
	}

	public void setEndPacketNum(long endPacketNum) {
		this.endPacketNum = endPacketNum;
	}

	public int getFrameState() {
		return frameState;
	}

	public void setFrameState(int frameState) {
		this.frameState = frameState;
	}

	/**用于储存一帧被接受到的所有数据包,用于组帧*/
	private Map<Long,PacketUdpBean> framesCache=new ConcurrentHashMap<>();



	public FrameBean(){

	}

	public void  addNewPacket(PacketUdpBean packetBean){
		if(packetBean==null){
			return;
		}

		long packetIndex=packetBean.getPacketIndex();
		if(!framesCache.containsKey(packetIndex)){
			if(frameState==FRAME_STATE_CREATE){
				this.frameIndex=packetBean.getFrameIndex();
				this.frameType=packetBean.getFrameType();
				this.packetCount=packetBean.getPackageCount();
				this.ibpType=packetBean.getIbpType();

				this.startPacketNum=packetBean.getPacketStartIndex();
				long endNum=(startPacketNum+packetCount)-1;

				/*if(endNum>MAX_PACKET_NUM){
					middlePacketNum=MAX_PACKET_NUM;
					this.endPacketNum=endNum-MAX_PACKET_NUM-1;

				}else if(endNum==MAX_PACKET_NUM){
					middlePacketNum=0;
					this.endPacketNum=endNum;

				}else{
					middlePacketNum=-1;
					this.endPacketNum=endNum;

				}*/

				middlePacketNum=-1;
				this.endPacketNum=endNum;

				this.frameLength=packetBean.getPacketLen();
				this.curPacketCount++;

				//add packet cache
				framesCache.put(packetIndex,packetBean);
				Log.e("FrameFactory","FRAME--CREATE packetIndex="+packetIndex+"  |endPacketNum="+endPacketNum);
				//into start state
				this.frameState=FRAME_START_WAIT;
			}else if(frameState==FRAME_START_WAIT){
				this.frameLength+=packetBean.getPacketLen();
				this.curPacketCount++;

				//add packet cache
				framesCache.put(packetIndex,packetBean);
				//Log.i("FrameFactory","FRAME-add-WAIT packetIndex="+packetIndex+"  |");
			}

			//Determine whether to receive all the data packets
			if(curPacketCount==packetCount){
				//Log.w("FrameFactory","FRAME--COMPLETE frameIndex="+frameIndex+"  |");
				this.frameState=FRAME_STATE_COMPLETE;
			}

			Log.e("FrameFactory","curPacketCount="+curPacketCount+"  | frameLength="+frameLength+"|frameIndex="+frameIndex+"--|packetIndex="+packetIndex);
		}

	}

	public boolean isPacketCanAdd(PacketUdpBean packetBean,long minPocketEdgeNum){
		if(packetBean==null){
			return false;
		}

		this.frameIndex=packetBean.getFrameIndex();
		long packetIndex=packetBean.getPacketIndex();
		this.packetCount=packetBean.getPackageCount();
		this.startPacketNum=packetBean.getPacketStartIndex();
		long endNum=(startPacketNum+packetCount)-1;

		if(endNum>MAX_PACKET_NUM){
			middlePacketNum=MAX_PACKET_NUM;
			this.endPacketNum=endNum-MAX_PACKET_NUM-1;

		}else if(endNum==MAX_PACKET_NUM){
			middlePacketNum=0;
			this.endPacketNum=endNum;

		}else{
			middlePacketNum=-1;
			this.endPacketNum=endNum;
		}

		if(middlePacketNum==-1&&packetIndex>=minPocketEdgeNum){//一般情况0-Max
			return true;
		}else if((middlePacketNum==MAX_PACKET_NUM)){//包含循环归0
			if((packetIndex>=minPocketEdgeNum&&packetIndex<=MAX_PACKET_NUM)||packetIndex<=endPacketNum){
				return true;
			}else{
				return false;
			}
		}else if(middlePacketNum==0){//ednN刚好等于Max
			if((packetIndex>=minPocketEdgeNum&&packetIndex<=MAX_PACKET_NUM)){
				return true;
			}else{
				return false;
			}
		}else {
			return false;
		}

	}

	/**Clear all frame data from list*/
	public void clearFrameData(){
		if(framesCache!=null){
			framesCache.clear();
			framesCache=null;
		}

	}


	/**Group packet*/
	public byte[] startFraming(){
		//Log.e("lixby","startFraming-start-frameLength="+frameLength+"|-endPacketNum="+endPacketNum);

		int fLength= frameLength;
		byte[] frame=new byte[fLength];

		if(middlePacketNum==MAX_PACKET_NUM){
			int desPosition=0;
			for (long i = startPacketNum; i <=middlePacketNum ; i++) {
				PacketUdpBean packetBean = framesCache.get(i);
				int packetLength=packetBean.getPacketLen();
				System.arraycopy(packetBean.getPacketData(),0,frame,desPosition,packetLength);
				desPosition= desPosition+packetLength;

			}

			for (long j = 0; j <= endPacketNum; j++) {
				PacketUdpBean packetBean = framesCache.get(j);
				int packetLength=packetBean.getPacketLen();
				System.arraycopy(packetBean.getPacketData(),0,frame,desPosition,packetLength);
				desPosition= desPosition+packetLength;
			}

		}else{
			int desPosition=0;
			for (long i = startPacketNum; i <= endPacketNum; i++) {
				PacketUdpBean packetBean = framesCache.get(i);
				int packetLength=packetBean.getPacketLen();
				System.arraycopy(packetBean.getPacketData(),0,frame,desPosition,packetLength);
				desPosition= desPosition+packetLength;
			}
		}

		Log.e("FrameFactory","startFraming-end-framelen="+frame.length+"--|Is I="+isIFrame(frame));
		return frame;
	}

	private boolean isIFrame(byte[] data){
		return data[4] == 39;
	}

	/**Update period and check frame states*/
	public void updatePeriod() {
		if(getFrameState()==FRAME_START_WAIT){
			period=period-checkCycle;

			if(period<=0){
				if(getCurPacketCount()==getPacketCount()){
					this.frameState=FRAME_STATE_COMPLETE;
				}else{
					this.frameState=FRAME_STATE_DISCARD;
				}

			}
		}

	}


}
