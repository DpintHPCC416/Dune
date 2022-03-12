package Common;

import Sketch.Sketchlet;
import Sketch.SuMaxSketch;

public class Receiver {
    private SuMaxSketch sketch;
    public Receiver(SuMaxSketch sketch)
    {
        this.sketch = sketch;
    }

    public void receive_Sketchlet(Sketchlet sketchlet)
    {
        this.sketch.setBySketchlet(sketchlet);
    }

    public void Receive_Packet(Packet packet)
    {
        if(packet.INT == true)
            receive_Sketchlet(packet.sketchlet);
    }

    public SuMaxSketch getSketch()
    {
        return this.sketch;
    }
}
