package com.skylight.client.udp.mode;

/**
 * Description:
 * Author: Created by lixby on 18-1-12.
 */

public class StatisticsBean extends PacketBean {

    /**UDP head length*/
    private static final int PACKET_HEAD_LENGTH=18;

    private long packetIndex;

    private long packetLen;

    public StatisticsBean() {
    }

    public StatisticsBean(long packetIndex, long packetLen) {
        this.packetIndex = packetIndex;
        this.packetLen = packetLen;
    }

    public long getPacketIndex() {
        return packetIndex;
    }

    public void setPacketIndex(long packetIndex) {
        this.packetIndex = packetIndex;
    }

    public long getPacketLen() {
        return packetLen+PACKET_HEAD_LENGTH;
    }

    public void setPacketLen(long packetLen) {
        this.packetLen = packetLen;
    }
}
