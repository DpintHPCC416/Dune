package Sketch;

import Common.Constant;

public class FactoryColumnSketch implements FactorySketch{
    @Override
    public SuMaxSketch getSuMaxSketch() {
        return new SuMaxColumnLet(Constant.SUMAX_ARRAYS_NUM, Constant.SUMAX_ARRAY_BITNUM);
    }

    /**
     * 复用sketchlet
     * @return
     */
    @Override
    public Sketchlet getSketchLet() {
        return new Columnlet(Constant.SUMAX_ARRAYS_NUM);
    }

}
