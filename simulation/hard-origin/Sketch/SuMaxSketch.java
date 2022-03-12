package Sketch;
import Common.*;
import Common.Packet;
import Utility.Hash;
import Utility.Md5;
import Utility.Sha256;
import Utility.Util;

import java.util.List;

/**
 * Used Now.
 */

public abstract class SuMaxSketch {
    protected int[][] sizeCellArray;
    protected Hash[] hashes;
    protected int[] hashIndex;

    public int getSUMAX_ARRAY_LENGTH() {
        return SUMAX_ARRAY_LENGTH;
    }

    public int getRowNum() {
        return rowNum;
    }

    protected boolean[] isCellUsableArray;
    protected boolean isUsingTheda;
    protected int ArrayLengthBit;
    protected int SUMAX_ARRAY_LENGTH;
    protected int rowNum;

    /**
     * 需要覆写
     * @param sketchlet
     */
    public abstract void setBySketchlet(Sketchlet sketchlet);

    /**
     * 需要覆写
     * @return
     */
    public abstract Sketchlet getSketchlet(Sketchlet sketchlet);


    public SuMaxSketch(int rowNum, int bitNum)
    {
        this.rowNum = rowNum;
        this.ArrayLengthBit = bitNum;
        this.SUMAX_ARRAY_LENGTH = (1 << this.ArrayLengthBit);
        this.sizeCellArray = new int[this.rowNum][this.SUMAX_ARRAY_LENGTH];
        this.hashes = new Hash[this.rowNum];
        this.hashes[0] = new Md5();
        this.hashes[1] = new Sha256();
        this.hashIndex = new int[this.rowNum];
     }


    public void setHashes(Hash[] hashes) {
        this.hashes = hashes;
    }

    /**
     * 使用这个函数表明启用theda
     * @param theda
     */
    public SuMaxSketch(int rowNum, int bitNum,double theda)
    {
        this(rowNum, bitNum);
        initCellUsableArray(theda);
    }

    /**
     * @param theda theda 是相对于所有的column而言的比例
     */
    private void initCellUsableArray(double theda)
    {
        isCellUsableArray = new boolean[this.SUMAX_ARRAY_LENGTH];
        isUsingTheda = true;
        List<Integer> usableIndicators = Util.getRandomList(this.SUMAX_ARRAY_LENGTH, (int)(theda * this.SUMAX_ARRAY_LENGTH));
        for(Integer i: usableIndicators)
        {
            isCellUsableArray[i] = true;
        }
    }


    public void Insert(Packet packet, TelemetryData data) {
        setHashIndexByPacketID(packet.getFlowID());
        handleSizeArray(data.packetSize);
    }

    public void setHashIndexByPacketID(FlowID flowID) {
        if(!flowID.hasReservedHashValues())
        {
            byte[] idBytes = flowID.toBytes();
            for(int i = 0; i < this.rowNum; i++)
            {
                hashIndex[i] = hashes[i].run(idBytes);
            }
        }
        else
        {
            int[] tmpHashValues = flowID.getHashValues();
            for(int i = 0; i < this.rowNum; i++)
            {
                hashIndex[i] = tmpHashValues[i];
            }
        }
    }

    public abstract void handleSizeArray(int value);


    public QueryData query(FlowID id, QueryType type) {
        switch (type)
        {
            case QUERY_SIZE:
                return querySizeArray(id);
        }
        return null;
    }

    /**
     * 查询sketch, 将查询到的值都返回
     * @param flowID
     * @return
     */
    public QueryData querySizeArray(FlowID flowID)
    {
        setHashIndexByPacketID(flowID);
        QueryFlowSizeData data = new QueryFlowSizeData(this.rowNum);
        for(int i = 0; i < this.rowNum; i++)
        {
            int index = this.hashIndex[i] % this.SUMAX_ARRAY_LENGTH;
            data.setFlowSize(i, sizeCellArray[i][index]);
        }
        return data;
    }

    public int[][] getSizeCellArray() {
        return sizeCellArray;
    }
}
