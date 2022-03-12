package Common;

import Utility.Hash;

public class FlowID {
    public long srcIP; // 4 bytes in real
    public long dstIP; // 4 bytes in real
    public int srcPort; // 2 bytes in real
    public int dstPort; // 2 bytes in real
    public int protocol; // 1 bytes in real
    private int[] hashValues; //预存hash值
    public FlowID(long srcIP, long dstIP, int srcPort, int dstPort, int protocol)
    {
        this.srcIP = srcIP;
        this.dstIP = dstIP;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.protocol = protocol;
        this.protocol = protocol;
    }

    public byte[] toBytes()
    {
        byte[] bytes = new byte[13];
        bytes[0] = (byte) ((srcIP) & 0xff);
        bytes[1] = (byte) ((srcIP >> 8) & 0xff);
        bytes[2] = (byte) ((srcIP >> 16) & 0xff);
        bytes[3] = (byte) ((srcIP >> 24) & 0xff);

        bytes[4] = (byte) ((dstIP) & 0xff);
        bytes[5] = (byte) ((dstIP >> 8) & 0xff);
        bytes[6] = (byte) ((dstIP >> 16) & 0xff);
        bytes[7] = (byte) ((dstIP >> 24) & 0xff);

        bytes[8] = (byte) ((srcPort) & 0xff);
        bytes[9] = (byte) ((srcPort >> 8) & 0xff);

        bytes[10] = (byte) ((dstPort) & 0xff);
        bytes[11] = (byte) ((dstPort >> 8) & 0xff);

        bytes[12] = (byte) ((protocol) & 0xff);

        return bytes;
    }

    /**
     * 预存hash值，避免每个包都要做hash运算
     * @param hashes hash函数集合
     */
    public void setHashValues(Hash[] hashes) {
        int hashLen = hashes.length;
        this.hashValues = new int[hashLen];
        for(int i = 0; i < hashLen; i++)
        {
            this.hashValues[i] = hashes[i].run(this.toBytes());
        }
    }

    /**
     * 是否已经计算过hash值
     * @return
     */
    public boolean hasReservedHashValues()
    {
        return (hashValues != null);
    }

    public int[] getHashValues() {
        return hashValues;
    }
}
