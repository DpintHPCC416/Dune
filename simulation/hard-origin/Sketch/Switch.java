package Sketch;


import Common.*;


public class Switch {
    private SuMaxSketch sketch;
    private TelemetryData data;
    public Switch(SuMaxSketch sketch)
    {
        this.sketch = sketch;
        data = new TelemetryData();
    }


    public SuMaxSketch getSketch() {
        return sketch;
    }

    /**
     * 从外部直接调用process，端口上没有线程接收
     * @param packet
     * @return
     */
    public boolean process(Packet packet, Sketchlet sketchlet)
    {
        data.packetSize = 1;
        if(packet.INT == false)
        {
            sketch.Insert(packet, data); //不是INT流才插入数据
        }
        //如果是INT流，执行INT
        if(packet.INT == true)
        {
            packet.sketchlet = sketch.getSketchlet(sketchlet);
        }
        return true;
    }

    public QueryData querySketch(FlowID id, QueryType type)
    {
        return sketch.query(id, type);
    }
}

