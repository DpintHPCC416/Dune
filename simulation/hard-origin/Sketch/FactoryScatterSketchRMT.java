package Sketch;

import Common.Constant;

/**
 * 使用构造器模式
 */
public class FactoryScatterSketchRMT implements FactorySketch{
    private SuMaxScatterLetRMT scatterLetRMT = null;
    public void reset()
    {
        scatterLetRMT = null;
    }
    public void setSketch(int rowNum, int bitNum)
    {
        this.scatterLetRMT = new SuMaxScatterLetRMT(rowNum, bitNum);
    }
    public void setMask(int totalBits, int leftSetBits)
    {
        System.out.println("Using mask");
        this.scatterLetRMT.setMask(new Mask(totalBits, leftSetBits));
    }
    public void setCookie(int cookieTotalBits)
    {
        System.out.println("Using Cookie");
        this.scatterLetRMT.setCookie(cookieTotalBits);
    }
    public void setSelfAdjust(double adjustLowBound, double adjustUpBound, int adjustInterval)
    {
        System.out.println("Using self-adjusting");
        if(this.scatterLetRMT.getMask() != null)
        {
            this.scatterLetRMT.setSelfAdjust(adjustLowBound, adjustUpBound, adjustInterval);
        }
    }
    @Override
    public SuMaxSketch getSuMaxSketch() {
        return scatterLetRMT.clone();
    }

    @Override
    public Sketchlet getSketchLet() {
        return new Scatterlet(Constant.SUMAX_ARRAYS_NUM);
    }
}