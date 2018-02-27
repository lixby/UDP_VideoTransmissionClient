package com.skylight.client.udp.mode;


/**
 * Record each packet of data information
 */
public class PacketBean {



	/**该包的数据*/
	public byte[] packetData;

	public byte[] getPacketData() {
		return packetData;
	}

	public void setPacketData(byte[] packetData) {
		this.packetData = packetData;
	}

	/**
	 * 计算对应byte[]->long
	 * @param byteNum
	 * @param offset
	 * @param byteSize
	 * @return
	 */
	public long bytes2Long(byte[] byteNum,int offset,int byteSize) {
		long num = 0;
		for (int ix = offset; ix < offset + byteSize; ix++) {
			num <<= 8;
			num |= (byteNum[ix] & 0xff);
		}
		return num;
	}

	/**
	 * 计算对应byte[]->int
	 * @param byteNum
	 * @param offset
	 * @param byteSize
	 * @return
	 */
	protected int bytes2Int(byte[] byteNum,int offset,int byteSize) {
		int num = 0;
		for (int ix = offset; ix < offset + byteSize; ix++) {
			num <<= 8;
			num |= (byteNum[ix] & 0xff);
		}
		return num;
	}


}
