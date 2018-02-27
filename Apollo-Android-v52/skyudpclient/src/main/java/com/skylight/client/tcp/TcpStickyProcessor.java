package com.skylight.client.tcp;

import android.util.Log;

import com.skylight.client.tcp.mode.TlvMode;
import com.skylight.client.tcp.mode.PackageBean;
import com.skylight.util.TcpUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * Description:
 * Author: Created by lixby on 18-1-19.
 */

public class TcpStickyProcessor {


    private String TAG = "howardTcpPackage2:";
    public static final int HEAD_FLAG_LENGTH = 4;
    /**
     * 缓存数据,用于存放半头数据
     */
    public byte[] cacheData;
    public byte[] tempCache;
    private int position = 0;
    public boolean isCompletePackage = false;//是否完整包
    private boolean getPackageLen = false;
    public final int HEADLENGTH = 8;
    public int packetLens = -1;
    private int mPacketLens;

    private SplitPacketListener splitPacketListener;

    public TcpStickyProcessor() {

    }

    public SplitPacketListener getSplitPacketListener() {
        return splitPacketListener;
    }

    public void setSplitPacketListener(SplitPacketListener splitPacketListener) {
        this.splitPacketListener = splitPacketListener;
    }

    /**
     * Sticky packet processing data output callback class
     */
    public interface SplitPacketListener {
        void splitCompleted(PackageBean packageBean);
    }

    /**
     *
     *<Pre>
     * Head information：
     * Head length=8
     * |--4字节(标记0x15(21))---|--1字节（Common type）----|
     *
     * |--1字节（ Packet type）--|--2字节（Packet length）--|
     *
     * |--------------content（携带数据）------------------|
     *
     * 第1-4字节：标记0x15，			|00 00 00 0x15|
     *
     * 第5字节：Common type    		命令类型
     *
     * 第6字节：  packet type			0x40发送包
     * 							    0x41 响应包
     *
     * 第7,8字节：Content length  	负载长度
     *
     * Content information：携带内容数据，为TLV格式：
     *
     * |--T(2byte)--|---L(2byte)---|
     * |------------V--------------|
     *
     * |--T(2byte)--|---L(2byte)---|
     * |------------V--------------|
     *
     *			  ......
     *
     *TLV格式定义如下：
     *	1)T：Type 数据类型
     *
     * 	2)L：Length Value的长度
     *
     *  3)V：value 携带的数据
     *</Pre>
     */
    public void splitTcpPacket(byte[] source) {
        int lenStart = TcpUtils.findDatahead(source, 0);
        boolean startWhile = false;

        if (lenStart != -1) {
            System.out.println("parsedata ------1------");
            if (lenStart != 0) {
                add(source, 0, lenStart);
                startWhile = true;

            }else{
                System.out.println("parsedata ------2------");
                reset();

                if (source.length>HEADLENGTH){
                    mPacketLens = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, 6, 0, 2))+HEADLENGTH;
                }

                lenStart = TcpUtils.findDatahead(source, HEAD_FLAG_LENGTH);
                while (mPacketLens!=0 && lenStart!=-1 && lenStart<mPacketLens){
                    System.out.println("parsedata ------17------");
                    lenStart = TcpUtils.findDatahead(source, lenStart+1);
                }

                if(lenStart!=-1){
                    System.out.println("parsedata ------3------");
                    //这是一个完整的包
                    mPacketLens = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, 6, 0, 2))+HEADLENGTH;
                    byte[] packageData=new byte[mPacketLens];
                    System.arraycopy(source, 0, packageData, 0, mPacketLens);
                    PackageBean packageBean=parseTLV(packageData);
                    startWhile = true;

                }else{
                    System.out.println("parsedata ------4------");
                    if (source.length >= HEADLENGTH) {
                        System.out.println("parsedata ------5------");
                        int packetLens  = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, 6, 0, 2))+HEADLENGTH;
                        System.out.println("packetLens ------5------"+packetLens);

                        if(source.length <packetLens){
                            System.out.println("parsedata ------6------");
                            // 当前数据小于发过来的整包长度 ,将当前整段数据加入半包中
                            add(source, 0, source.length);

                        }else{
                            System.out.println("parsedata ------7------");
                            byte[] packageData=new byte[packetLens];
                            System.arraycopy(source, 0, packageData, 0, packetLens);
                            PackageBean packageBean=parseTLV(packageData);
                            if(source.length-packetLens>0){
                                System.out.println("parsedata ------8------");
                                //如果数据比一个完整的包还大，说明还有剩余
                                reset();
                                add(source, packetLens, source.length);
                            }

                        }
                    }else{
                        System.out.println("parsedata ------9------");
                        add(source, 0, source.length);

                    }
                    startWhile = false;

                }
            }

            while (startWhile) {
                if (source.length>lenStart+HEADLENGTH){
                    mPacketLens  = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, lenStart+6, 0, 2))+HEADLENGTH;
                }

                int midLenStart = TcpUtils.findDatahead(source, lenStart + HEADLENGTH);
                while (mPacketLens!=0 && midLenStart!=-1 && midLenStart-lenStart<mPacketLens){
                    System.out.println("parsedata ------18------");
                    byte[] bytes=new byte[source.length-lenStart];
                    System.arraycopy(source, lenStart, bytes, 0, source.length-lenStart);
                    System.out.println("howardparsedata = "+ Arrays.toString(bytes));
                    midLenStart = TcpUtils.findDatahead(source, midLenStart+1);

                }

                if (midLenStart != -1) {
                    System.out.println("parsedata ------10------");
                    mPacketLens  = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, lenStart+6, 0, 2))+HEADLENGTH;
                    byte[] packageData=new byte[mPacketLens];
                    //					System.arraycopy(source, lenStart, packageData, 0, midLenStart-lenStart);
                    System.arraycopy(source, lenStart, packageData, 0, mPacketLens);
                    parseTLV(packageData);
                    lenStart=midLenStart;

                }else{
                    System.out.println("parsedata ------11------");
                    System.out.println("parsedata mPacketLens = "+mPacketLens);
                    System.out.println("parsedata LEN = "+(source.length-lenStart));
                    if (source.length-lenStart >= HEADLENGTH) {
                        System.out.println("parsedata ------12------");
                        int packetLens  = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, lenStart+6, 0, 2))+HEADLENGTH;
                        if(source.length-lenStart <packetLens){
                            reset();
                            System.out.println("parsedata ------13------");
                            // 当前数据小于发过来的整包长度 ,将当前整段数据加入半包中
                            add(source, lenStart, source.length);

                        }else{
                            System.out.println("parsedata ------14------");
                            byte[] packageData=new byte[packetLens];
                            System.arraycopy(source, lenStart, packageData, 0, packetLens);
                            PackageBean packageBean=parseTLV(packageData);

                            if(source.length-lenStart-packetLens>0){
                                //如果数据比一个完整的包还大，说明还有剩余
                                reset();
                                add(source, packetLens+lenStart+packetLens, source.length);
                            }

                        }
                    }else{
                        System.out.println("parsedata ------15------");
                        reset();
                        add(source, lenStart, source.length);

                    }
                    startWhile=false;

                }
            }
        }else{
            System.out.println("parsedata ------16------");
            add(source, 0, source.length);
        }

    }



    private void add(byte[] source, int start, int end) {
        int len = end - start;
        if (!getPackageLen && len > 0 && len < 8) {
            if (tempCache == null) {
                tempCache = new byte[len];
                System.arraycopy(source, 0, tempCache, 0, len);
            }

        } else if (!getPackageLen && len > 7) {
            byte[] newData=new byte[7];
            System.arraycopy(source, start, newData, 0, 7);
            System.out.println("howard tcppackage = "+ Arrays.toString(newData));
            Log.d(TAG, "tcppackage packetLens = " + packetLens);
            if (tempCache != null && tempCache.length > 0) {
                byte[] bytes = TcpUtils.byteMerger(tempCache, source, source.length);
                packetLens = TcpUtils.byteArr2int(TcpUtils.byteMerger(bytes, start + 6, 0, 2)) + HEADLENGTH;
                getPackageLen = true;
                cacheData = new byte[packetLens];
                tempCache = null;
                if(packetLens>bytes.length){
                    System.arraycopy(bytes, start, cacheData, 0, packetLens);
                    position += packetLens;
                }else {
                    cacheData = new byte[bytes.length];
                    packetLens=bytes.length;
                    System.arraycopy(bytes, start, cacheData, 0, bytes.length);
                    position += bytes.length;
                }


            } else {
                packetLens = TcpUtils.byteArr2int(TcpUtils.byteMerger(source, start + 6, 0, 2)) + HEADLENGTH;
                getPackageLen = true;
                cacheData = new byte[packetLens];
                if (getPackageLen && position + len <= packetLens) {
                    System.arraycopy(source, start, cacheData, 0, len);
                    position += len;
                }

            }

        } else if (getPackageLen && position + len <= packetLens) {
            System.arraycopy(source, start, cacheData, position, len);
            position += len;
        } else if (getPackageLen && position + len > packetLens) {
            //TODO
            System.out.println("error error error error error error");
        }

        Log.d(TAG, "tcppackage position = " + position);
        if (getPackageLen && position >= packetLens) {
            if (splitPacketListener != null) {
                position = 0;
                getPackageLen = false;
                PackageBean packageBean = parseTLV(cacheData);
                splitPacketListener.splitCompleted(packageBean);
            }

        }

    }

    private void reset() {
        position = 0;
        isCompletePackage = false;
        getPackageLen = false;
        cacheData = null;
    }

    private PackageBean parseTLV(byte[] packageData) {
        Log.d(TAG, packageData.toString());
        System.out.println("howard22222" + Arrays.toString(packageData));
        PackageBean packageBean = new PackageBean();
        packageBean.commandType = TcpUtils.byteArr2int(TcpUtils.byteMerger(packageData, 4, 0, 1));
        Log.d(TAG, "packageBean.commandType = " + packageBean.commandType);
        packageBean.packageType = TcpUtils.byteArr2int(TcpUtils.byteMerger(packageData, 5, 0, 1));
        Log.d(TAG, "packageBean.packageType = " + packageBean.packageType);

        int packageLength = TcpUtils.byteArr2int(TcpUtils.byteMerger(packageData, 6, 0, 2)) + HEADLENGTH;
        Log.d(TAG, "packageLength = " + packageLength);
        int parsePosition = HEADLENGTH;
        while (parsePosition < packageLength) {
            TlvMode tlvBean = new TlvMode();
            int type = TcpUtils.byteArr2int(TcpUtils.byteMerger(packageData, parsePosition, 0, 2));
            tlvBean.setType(type);
            parsePosition += 2;
            Log.d(TAG, "tlv type = " + type);
            int tlvlen = TcpUtils.byteArr2int(TcpUtils.byteMerger(packageData, parsePosition, 0, 2));
            tlvBean.setLength(tlvlen);
            parsePosition += 2;
            Log.d(TAG, "tlv tlvlen = " + tlvlen);
            byte[] tlvData = TcpUtils.byteMerger(packageData, parsePosition, 0, tlvlen);
            tlvBean.setTlvData(tlvData);
            parsePosition += tlvlen;
            System.out.println("howard33333  = " + Arrays.toString(tlvData));
            Log.d(TAG, "tlv tlvData length = " + tlvData.length);
            if (type == 0x605 || type == 0x606) {
                Log.d(TAG, "tlv tlvData = " + tlvData[0] + tlvData[1] + tlvData[2]);
            }
            if (type == 0x600) {
                try {
                    String ip = InetAddress.getByAddress(tlvData).toString();
                    Log.d(TAG, "tlv ip  = " + ip);
                    System.out.println();
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (type == 0x601) {
                int port = TcpUtils.byteArr2int(tlvData);
                System.out.println("tlv port = " + port);
            }

            packageBean.tlvCache.add(tlvBean);
        }

        if(splitPacketListener!=null){
            splitPacketListener.splitCompleted(packageBean);
        }

        return packageBean;
    }

}



