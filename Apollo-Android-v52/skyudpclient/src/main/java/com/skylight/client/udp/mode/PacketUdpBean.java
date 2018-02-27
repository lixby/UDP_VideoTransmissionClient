package com.skylight.client.udp.mode;


import com.skylight.util.Logger;

/**
 *Description:
 *Author: Created by lixby on 17-12-28.
 *
 * Parse udp packet head data,
 * Header data structure:
 *<Pre>
 * |--Flat(2byte)--|--PacketIndex(4byte)--|--PacketLen(2byte)--|--FrameIndex(2byte)--|
 * |---FrameType(1byte)---|----PacketStartIndex(4byte)----|----PacketCount(2byte)----|
 * |----IBP type(1byte)---|
 *
 * Flat:udp head标记， 占2byte
 * PacketIndex：当前Video/Audio 的序号， 占4字节
 * PacketLen：携带数据包长度， 占2字节
 * FrameIndex：packet所属帧的序号， 占2字节
 * FrameType：帧类型（视频/音频）， 占1字节 ,0X0a视频,0x14音频
 * PacketStartIndex：packet开始的序号值， 占4字节
 * PacketCount：所属当前帧的packet数量， 占2字节
 * IBP type: Frame 类型，I帧或者P帧等，占1字节
 *
 *</Pre>
 */

public class PacketUdpBean extends PacketBean{

    /**UDP Stream head tag*/
    public static final long PACKET_TAG=0x16;
    /**UDP head length*/
    public static final int PACKET_HEAD_LENGTH=18;

    /**Flat*/
    public static final int HEAD_TAG_LEN=2;
    /**PacketIndex*/
    public static final int HEAD_PKINDEX_LEN=4;
    /**PacketLen*/
    public static final int HEAD_PKLEN_LEN=2;
    /**FrameIndex*/
    public static final int HEAD_FMINDEX_LEN=2;
    /**FrameType*/
    public static final int HEAD_FMTYPE_LEN=1;
    /**PacketStartIndex*/
    public static final int HEAD_PKSI_LEN=4;
    /**PacketCount*/
    public static final int HEAD_PKCOUNT_LEN=2;
    /**IBP type*/
    public static final int HEAD_IBP_LEN=1;


    /**该包的包序号*/
    public long packetIndex;
    /**该包的包长*/
    public int packetLen;
    /**该包的所在帧的序号*/
    public int frameIndex;
    /**帧类型（视频和音频）*/
    public int frameType;
    /**包所属帧的包开始的第一个包的序号*/
    public int packetStartIndex;
    /**包所属帧的包的数量*/
    public int packageCount;
    /**帧类型，I帧或者P帧*/
    public int ibpType;


    public PacketUdpBean() {
    }

    public PacketUdpBean (int packetIndex,
                          int packetLen,
                          int frameIndex,
                          int frameType,
                          int packetStartIndex,
                          int packageCount,
                          byte[] packetData){
    }


    public long getPacketIndex() {
        return packetIndex;
    }

    public void setPacketIndex(int packetIndex) {
        this.packetIndex = packetIndex;
    }

    public int getPacketLen() {
        return packetLen;
    }

    public void setPacketLen(int packetLen) {
        this.packetLen = packetLen;
    }

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

    public int getPacketStartIndex() {
        return packetStartIndex;
    }

    public void setPacketStartIndex(int packetStartIndex) {
        this.packetStartIndex = packetStartIndex;
    }

    public int getPackageCount() {
        return packageCount;
    }

    public void setPackageCount(int packageCount) {
        this.packageCount = packageCount;
    }

    public int getIbpType() {
        return ibpType;
    }

    public void setIbpType(int ibpType) {
        this.ibpType = ibpType;
    }


    public long getPacketTag(byte[] packet){
        return bytes2Long(packet, 0 , HEAD_TAG_LEN);
    }

    public long getPacketTag(){
        return bytes2Long(packetData, 0 , HEAD_TAG_LEN);
    }

    public long getPacketIndex(byte[] packet){
        return bytes2Long(packet, 2 , HEAD_PKINDEX_LEN);
    }

    public long getPacketLen(byte[] packet){
        return bytes2Long(packet, 6 , HEAD_PKLEN_LEN)+PACKET_HEAD_LENGTH;
    }

    public  boolean parsePacket(byte[] packageData,long packetWindowNum){
        int offset=0;
        int flag = bytes2Int(packageData, offset , HEAD_TAG_LEN);

        if(flag!=PACKET_TAG){
            return false;
        }

        offset=offset+HEAD_TAG_LEN;
        long packet_Index = bytes2Long(packageData, offset , HEAD_PKINDEX_LEN);
        if(packet_Index<packetWindowNum){
            return false;
        }

        offset=offset+HEAD_PKINDEX_LEN;
        int packet_Len = bytes2Int(packageData,  offset, HEAD_PKLEN_LEN);

        offset=offset+HEAD_PKLEN_LEN;
        int frame_Index = bytes2Int(packageData, offset , HEAD_FMINDEX_LEN);

        offset=offset+HEAD_FMINDEX_LEN;
        int frame_Type = bytes2Int(packageData, offset , HEAD_FMTYPE_LEN);

        offset=offset+HEAD_FMTYPE_LEN;
        int packet_StartIndex = bytes2Int(packageData, offset , HEAD_PKSI_LEN);

        offset=offset+HEAD_PKSI_LEN;
        int package_Count = bytes2Int(packageData, offset , HEAD_PKCOUNT_LEN);

        offset=offset+HEAD_PKCOUNT_LEN;
        int ibp_Type = bytes2Int(packageData, offset , HEAD_IBP_LEN);

        Logger.d("head--flag:" + flag + "|packet_Index:" + packet_Index + "|packet_Len:" + packet_Len+ "|frame_Index:" + frame_Index
                + "|frame_Type:" + frame_Type + "|packet_StartIndex:"+ packet_StartIndex + "|package_Count:" + package_Count+
                "|ibp_Type:" + ibp_Type+"|");

        byte[] content=new byte[packet_Len];
        System.arraycopy(packageData,PACKET_HEAD_LENGTH,content,0,packet_Len);

        this.packetIndex=packet_Index;
        this.packetLen=packet_Len;
        this.frameIndex=frame_Index;
        this.frameType=frame_Type;
        this.packetStartIndex=packet_StartIndex;
        this.packageCount=package_Count;
        this.ibpType=ibp_Type;
        this.packetData=content;


        return true;
    }



    public  boolean parsePacket(byte[] packageData){
        int offset=0;
        int flag = bytes2Int(packageData, offset , HEAD_TAG_LEN);

        if(flag!=PACKET_TAG){
            return false;
        }

        offset=offset+HEAD_TAG_LEN;
        long packet_Index = bytes2Long(packageData, offset , HEAD_PKINDEX_LEN);

        offset=offset+HEAD_PKINDEX_LEN;
        int packet_Len = bytes2Int(packageData,  offset, HEAD_PKLEN_LEN);

        offset=offset+HEAD_PKLEN_LEN;
        int frame_Index = bytes2Int(packageData, offset , HEAD_FMINDEX_LEN);

        offset=offset+HEAD_FMINDEX_LEN;
        int frame_Type = bytes2Int(packageData, offset , HEAD_FMTYPE_LEN);

        offset=offset+HEAD_FMTYPE_LEN;
        int packet_StartIndex = bytes2Int(packageData, offset , HEAD_PKSI_LEN);

        offset=offset+HEAD_PKSI_LEN;
        int package_Count = bytes2Int(packageData, offset , HEAD_PKCOUNT_LEN);



        offset=offset+HEAD_PKCOUNT_LEN;
        int ibp_Type = bytes2Int(packageData, offset , HEAD_IBP_LEN);

        Logger.d("head--flag:" + flag + "|packet_Index:" + packet_Index + "|packet_Len:" + packet_Len+ "|frame_Index:" + frame_Index
                + "|frame_Type:" + frame_Type + "|packet_StartIndex:"+ packet_StartIndex + "|package_Count:" + package_Count+
                "|ibp_Type:" + ibp_Type+"|");

        byte[] content=new byte[packet_Len];
        System.arraycopy(packageData,PACKET_HEAD_LENGTH,content,0,packet_Len);

        this.packetIndex=packet_Index;
        this.packetLen=packet_Len;
        this.frameIndex=frame_Index;
        this.frameType=frame_Type;
        this.packetStartIndex=packet_StartIndex;
        this.packageCount=package_Count;
        this.ibpType=ibp_Type;
        this.packetData=content;


        return true;
    }

}
