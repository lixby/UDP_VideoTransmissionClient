package com.skylight.client.udp;

import android.util.Log;
import com.skylight.client.tcp.mode.TcpIpInformation;
import com.skylight.util.Logger;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class UdpStreamClient implements Runnable{

	private ExecutorService service;
	private boolean isRunning=false;
	private Future future;

	private String TAG = "UdpClient";

	/**Receive buffer length*/
	//private static final int PACKAGE_LENS = 1448;

	//private static final int PACKAGE_LENS = 8*1024;
	private static final int PACKAGE_LENS = 4*1024;
	//private static final int PACKAGE_LENS = 4*1472;

	/**the specified timeout in milliseconds*/
	private static final int OUT_TIME=30000;
	private DatagramSocket socket;
	private InetAddress serverAddress = null;

	public UdpStreamClient(){

	}

	/**Init upd and start*/
	public void startRun(){
		if(isRunning){
			return;
		}

		isRunning=true;
		service= Executors.newCachedThreadPool();
		//创建套接字
		socket = createSocket();
		future=service.submit(this);

	}

	/**Stop upd*/
	public void stopRun(){
		if(!isRunning){
			return;
		}

		if(socket!=null&&!socket.isClosed()){
			socket.close();
			socket=null;
		}

		if(future!=null){
			future.cancel(true);
			future=null;
		}

		if(service!=null){
			service.shutdownNow();
			service=null;
		}

	}

	public void release(){

	}

	public void run(){
		//接收返回数据
		while(isRunning){
			if(socket!=null){
				try {
					byte[] backBuffer = new byte[PACKAGE_LENS];
					DatagramPacket backPacket = createReceivePacket(backBuffer);
					socket.receive(backPacket);

					//receive stream data
					if(statusCallback!=null){
						int length=backPacket.getLength();
						byte[] receiveBuffer=Arrays.copyOf(backPacket.getData(),length);
						statusCallback.receive(receiveBuffer);
						//TimeUnit.NANOSECONDS.sleep(1000);
					}
					backPacket=null;
				}
				/*catch (InterruptedException e) {
					e.printStackTrace();
				}*/
				catch (SocketTimeoutException e) {

				}catch (SocketException e) {
					e.printStackTrace();
				}catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

		System.out.println("UdpClient thread end");
	}

	/**create DatagramSocket*/
	private DatagramSocket createSocket(){
		DatagramSocket socket= null;
		try {
			//Create random idle DatagramSocket
			socket = getRandomPort();
			socket.setReceiveBufferSize(4*1024*1024);
			int udp_port=socket.getLocalPort();
			TcpIpInformation.getInstance().setClientUdp_Port(udp_port);

			//Get local IP address
			String ip=TcpIpInformation.getInstance().getLocalAddress();
			if(statusCallback!=null){
				statusCallback.connected(udp_port,ip);
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return socket;
	}


	/**
	 * Send packet data.
	 * @param data
	 */
	public void sendPacket(byte[] data){
		try {
			DatagramPacket datagramPacket=createSendPacket(data);
			if(!socket.isClosed()){
				long m=System.currentTimeMillis();
				socket.send(datagramPacket);
				Logger.d("sendPacket-UDP--finished="+(System.currentTimeMillis()-m));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**create send DatagramPacket*/
	private DatagramPacket createSendPacket(byte[] data){
		DatagramPacket dataGramPacket=null;
		try {
			if(serverAddress==null){
				String serverIP=TcpIpInformation.getInstance().getServerUdp_IP();
				serverAddress = InetAddress.getByName(serverIP);
			}

			//创建发送方的数据报信息
			Logger.d("sendPacket-UDP--createSendPacket="+data.length);
			dataGramPacket = new DatagramPacket(data, data.length, serverAddress, TcpIpInformation.getInstance().getServerUdp_Port());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return  dataGramPacket;
	}


	/**create receive DatagramPacket*/
	private DatagramPacket createReceivePacket(byte[] data){
		//接收服务器反馈数据
		DatagramPacket backPacket = new DatagramPacket(data, data.length);
		return backPacket;
	}

	/**创建可用DatagramSocket*/
	private  DatagramSocket getRandomPort() throws IOException {
		int start_port=3364;
		int max_port=65535;

		for (int i = start_port; i <= max_port; i++) {
			try {
				Log.i(TAG,"random port="+i);
				return new DatagramSocket(i);
			} catch (SocketException e) {
				continue;
			}

		}

		throw new IOException("NO free port found");
	}


	private UDPStatusCallback statusCallback;

	public void setStatusCallback(UDPStatusCallback statusCallback) {
		this.statusCallback = statusCallback;
	}

	public interface  UDPStatusCallback{
		/**
		 * Output port number after successful connection
		 * @param port
		 */
		void connected(int port, String ip);

		/**
		 * Succeddful receiving data from the opposition sender
		 * @param packet
		 */
		void receive(byte[] packet);
	}


}