package Sketch;

import Common.Constant;

public class FactoryScatterSketch implements FactorySketch{
    @Override
    public SuMaxSketch getSuMaxSketch() {
        return new SuMaxScatterLet(Constant.SUMAX_ARRAYS_NUM, Constant.SUMAX_ARRAY_BITNUM);
    }

    @Override
    public Sketchlet getSketchLet() {
        return new Scatterlet(Constant.SUMAX_ARRAYS_NUM);
    }
}
