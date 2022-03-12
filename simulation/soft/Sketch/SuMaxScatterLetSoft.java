package Sketch;

import Common.Constant;
import Common.Cookie;
import Common.Mask;
import org.apache.log4j.Logger;

import java.util.Random;




public class SuMaxScatterLetSoft extends SuMaxSketch{
    private Random random;
    public Cookie[][] cookie;
    private Mask mask;
    private Logger logger = Logger.getLogger("SuMaxScatterLetRMT.class");

    public SuMaxScatterLetSoft clone()
    {
        return new SuMaxScatterLetSoft(this);
    }
    public Mask getMask() {
        return mask;
    }

    /**
     *
     * @param rowNum
     * @param bitNum
     */
    public SuMaxScatterLetSoft(int rowNum, int bitNum) {
        super(rowNum, bitNum);
        initRandom();
        System.out.println("This is SuMaxScatterLetSoft");
    }

    public SuMaxScatterLetSoft(SuMaxScatterLetSoft letSoft)
    {
        super(letSoft.rowNum, letSoft.ArrayLengthBit);
        initRandom();
        System.out.println("This is SuMaxScatterLetSoft");
        initCookie(letSoft.cookie[0][0].getCounterBits() + 1);
        setMask(new Mask(letSoft.mask.getTotalBits(), letSoft.mask.getLeftSetBits()));
        this.hashes = letSoft.hashes;
    }

    public void setCookie(int cookieTotalBits)
    {
        initCookie(cookieTotalBits);
    }
    public void setMask(Mask mask)
    {
        this.mask = mask;
    }


    @Override
    public void handleSizeArray(int value) {
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < Constant.SUMAX_ARRAYS_NUM; i++)
        {
            int index = this.hashIndex[i] % this.SUMAX_ARRAY_LENGTH;
            if(sizeCellArray[i][index] + value < min)
            {
                min = sizeCellArray[i][index] + value;
                sizeCellArray[i][index] = min;
                cookie[i][index].increment(Constant.INCREMENT_DELTA);
            }
            else
            {
                if(sizeCellArray[i][index] < min)
                {
                    sizeCellArray[i][index] = min;
                    cookie[i][index].increment(Constant.INCREMENT_DELTA);
                }
            }
        }
    }

    private void initRandom()
    {
        long seed = System.currentTimeMillis();
        if(Constant.DEBUG_FLAG)
            seed = 1;
        random = new Random(seed);
    }

    public void initCookie(int cookieTotalBits)
    {
        this.cookie = new Cookie[this.rowNum][this.SUMAX_ARRAY_LENGTH];
        for(int i = 0; i < rowNum; i++)
        {
            for(int j = 0; j < SUMAX_ARRAY_LENGTH; j++)
            {
                this.cookie[i][j] = new Cookie(cookieTotalBits - 1);
            }
        }
    }


    /**
     * 通过sketchlet更新sketch，只在终端进行，所以没有更新bitmap或cookie
     * @param sketchlet
     */
    public void setBySketchlet(Sketchlet sketchlet)
    {
        assert sketchlet instanceof SuMaxScatterLet;
        Scatterlet scatterlet = (Scatterlet) sketchlet;
        int absAddress = scatterlet.getAddress();
        for(int i = 0; i < this.rowNum; i++)
        {
            this.sizeCellArray[i][(absAddress + scatterlet.getOffsetByIndex(i)) % SUMAX_ARRAY_LENGTH] = scatterlet.getValueByIndex(i);
        }
    }

    /**
     * 判断第rowIndex行的[coladdrss, coladdrss + 2^r - 1]之间有没有可以用的值
     * @param rowIndex
     * @param colAddress
     * @param bound
     * @return
     */
    public boolean getOffsetByCookie(int rowIndex, int colAddress, int bound, Scatterlet scatterlet)
    {
        int tmpCol = 0;
        for(int i = 0; i < bound; i++)
        {
            int colIndex = (colAddress + i) % SUMAX_ARRAY_LENGTH;
            int val = cookie[rowIndex][colIndex].getValue();
            int tmp = mask.getMask() & val;
            if(tmp >= mask.getThreshold())
            {
                scatterlet.setOffsetByIndex(rowIndex, i);
                return true;
            }
            else if(tmp >= mask.getThreshold2())
            {
                tmpCol = i;
            }
        }
        //如果没有，选择最后一个有效的值
        scatterlet.setOffsetByIndex(rowIndex, tmpCol);
        return false;
    }

    @Override
    public Sketchlet getSketchlet(Sketchlet sketchlet) {
        return null;
    }
}
