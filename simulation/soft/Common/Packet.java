package Common;

import Sketch.Sketchlet;

public class Packet {
    //packet identifier
    private FlowID flowID;

    public FlowID getFlowID() {
        return flowID;
    }

    //other fields
    public int sequence;       //序列号，这条流的第几个包
    public Sketchlet sketchlet;     //该数据包所携带的sketchlet，交换机的处理程序对数据包的唯一修改就是赋予他一个sketchlet对象

    public boolean INT;

    public Packet(boolean INT)
    {
        this.INT= INT;
    }
    public void packetSet(FlowInfo info,int sequence,boolean INT)
    {
        this.flowID = info.flowID;
        this.sequence = sequence;
        this.INT= INT;
    }
    public void packetClear()
    {
        this.flowID = null;
        this.sequence = 0;
        this.INT= false;
        this.sketchlet = null;
    }
    public byte[] flowId2Bytes()
    {
        return (this.flowID == null) ? null : this.flowID.toBytes();
    }
}
