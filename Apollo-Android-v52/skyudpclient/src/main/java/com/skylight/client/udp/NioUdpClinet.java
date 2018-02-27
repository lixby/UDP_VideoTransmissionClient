package com.skylight.client.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * Description:
 * Author: Created by lixby on 18-1-23.
 */

public class NioUdpClinet {

    private Selector selector;
    private DatagramChannel datagramChannel;
    private ByteBuffer sendBuffer;
    private static final int BUFFER_LENGTH=6*1024;
    private boolean isRunning=false;



    public NioUdpClinet() {
        inItData();
        open();
    }

    private void inItData(){
        sendBuffer=ByteBuffer.allocate(BUFFER_LENGTH);
    }

    private void open(){
        try {
            //开启一个通道
            datagramChannel=DatagramChannel.open();
            //设置非阻塞模式
            datagramChannel.configureBlocking(false);
            //开启选择器
            selector=Selector.open();
            datagramChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void send(byte[] packet,String ip,int port){
        while (isRunning){
            try {
                int sNum=selector.select();
                if(sNum>0){
                    Iterator iterator=selector.selectedKeys().iterator();
                    while (iterator.hasNext()){
                        SelectionKey key= (SelectionKey) iterator.next();
                        if(key.isReadable()){
                            DatagramChannel channel= (DatagramChannel) key.channel();
                            sendPacket(channel,packet,new InetSocketAddress(ip,port));
                        }
                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**Send packet*/
    private  void sendPacket(DatagramChannel channel,byte[] packet,SocketAddress address){
        try {
            if(channel!=null&&channel.isOpen()&&sendBuffer!=null){
                sendBuffer.clear();
                sendBuffer.put(packet);
                channel.send(sendBuffer,address);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private class Sender implements Runnable{

        @Override
        public void run() {

        }

    }



    private class Receiver implements Runnable{

        @Override
        public void run() {

        }

    }
















}
