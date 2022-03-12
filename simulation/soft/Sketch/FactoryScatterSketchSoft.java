package Sketch;

import Common.Constant;
import Common.Mask;

public class FactoryScatterSketchSoft implements FactorySketch{
    private SuMaxScatterLetSoft scatterLetSoft = null;
    public void reset()
    {
        scatterLetSoft = null;
    }
    public void setSketch(int rowNum, int bitNum)
    {
        this.scatterLetSoft = new SuMaxScatterLetSoft(rowNum, bitNum);
    }
    public void setMask(int totalBits, int leftSetBits)
    {
        System.out.println("Using mask");
        this.scatterLetSoft.setMask(new Mask(totalBits, leftSetBits));
    }
    public void setCookie(int cookieTotalBits)
    {
        System.out.println("Using Cookie");
        this.scatterLetSoft.setCookie(cookieTotalBits);
    }
    @Override
    public SuMaxSketch getSuMaxSketch() {
        return scatterLetSoft.clone();
    }

    @Override
    public Sketchlet getSketchLet() {
        return new Scatterlet(Constant.SUMAX_ARRAYS_NUM);
    }
}
