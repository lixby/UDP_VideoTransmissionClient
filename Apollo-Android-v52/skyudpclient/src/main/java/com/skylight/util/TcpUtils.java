package com.skylight.util;

public class TcpUtils {

    public static int findDatahead(byte[] data,int start){
        if (data.length>8){
            for (int i=start;i < data.length - 6;i++){
                //        	System.out.println("findDatahead-"+i+":"+data[i] +".data.length:"+data.length);
                if (data[i]==00 && data[i+1]==00 && data[i+2] ==00 && data[i+3] ==21 && (data[i+5] == 64 || data[i+5] == 65)){
                    //            	System.out.println("findDatahead-"+i+".getHead");
                    return i;
                }
            }
        }
        return -1;
    }

    //System.arraycopy()方法
    public static synchronized byte[] byteMerger(byte[] bt1,int srcPos,int desPos,int lens){
        byte[] bt2 = new byte[lens];
        System.arraycopy(bt1, srcPos, bt2, desPos,lens);
        return bt2;
    }

    public static synchronized byte[] byteMerger(byte[] bt1,byte[] bt2,int spilt_lens){
        byte[] bt3 = new byte[bt1.length + spilt_lens];
        System.arraycopy(bt1, 0, bt3, 0,bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, spilt_lens);
        return bt3;
    }

    public static byte[] longToByteArr(long src){
        byte[] byteNum = new byte[8];
        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
        }
        return byteNum;
    }


    public static long bytes2Long(byte[] byteNum,int offset) {
        long num = 0;
        for (int ix = offset; ix < offset + 8; ix++) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }


    public static long bytes2Long(byte[] byteNum,int offset,int size) {
        long num = 0;
        for (int ix = offset; ix < offset + size; ix++) {
            num <<= 8;
            num |= (byteNum[ix] & 0xff);
        }
        return num;
    }


    public static byte[] data2byteArr(long src,int lens){
        byte[] byteNum = new byte[lens];
        for (int ix = 0; ix < lens; ix++) {
            int offset = 8 * (lens - ix - 1) ;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
            //	        System.out.println(byteNum[ix]);
        }
        return byteNum;
    }


    public static byte[] data2byteArr(int src,int lens){
        byte[] byteNum = new byte[lens];
        for (int ix = 0; ix < lens; ix++) {
            int offset = 8 * (lens - ix - 1) ;
            byteNum[ix] = (byte) ((src >> offset) & 0xff);
            //	        System.out.println(byteNum[ix]);
        }
        return byteNum;
    }


    public static int byteArr2int(byte[] src){
        int value=0;

        for(int i = 0; i < src.length; i++) {
            int shift= (src.length-1-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }
        return value;
    }


    public static int byteArr2int4(byte[] src,int offset){
        int value=0;

        for(int i = 0; i < offset + 4; i++) {
            int shift= (offset + 4-1-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }
        return value;
    }


    public static int byteArr2int2(byte[] src,int offset){
        int value=0;

        for(int i = offset; i < offset +2; i++) {
            int shift= (offset +1-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }
        return value;
    }


    public static long byteArr2long6(byte[] src,int offset){
        long value=0;
        for(int i = offset; i < offset +6; i++) {
            int shift= (offset +6-i) * 8;
            value +=(src[i] & 0x000000FF) << shift;
        }
        return value;
    }

}
