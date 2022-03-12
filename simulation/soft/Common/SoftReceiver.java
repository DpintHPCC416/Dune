package Common;

import Sketch.Sketchlet;
import Sketch.SoftSwitch;
import Sketch.SuMaxSketch;

public class SoftReceiver {
    private SuMaxSketch sketch;
    public SoftReceiver(SuMaxSketch sketch)
    {
        this.sketch = sketch;
    }

    public void receive_Sketchlet(Sketchlet sketchlet)
    {
        this.sketch.setBySketchlet(sketchlet);
    }

    public void Receive_Packet(Packet packet, SoftSwitch softSwitch)
    {
        if(packet.INT == true && packet.sketchlet != null)
        {
            receive_Sketchlet(packet.sketchlet);
            softSwitch.addToBuffer(packet.sketchlet);
            packet.sketchlet = null;
        }
    }

    public SuMaxSketch getSketch()
    {
        return this.sketch;
    }
}
