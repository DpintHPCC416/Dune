package Common;

import Sketch.Sketchlet;

public class Packet {
    //packet identifier
    private FlowID flowID;

    public FlowID getFlowID() {
        return flowID;
    }

    //other fields
    public int sequence;       //the packet serial number in this flow
    public Sketchlet sketchlet;    

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
