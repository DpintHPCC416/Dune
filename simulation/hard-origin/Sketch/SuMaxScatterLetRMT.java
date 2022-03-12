package Sketch;

import Common.Constant;
import org.apache.log4j.Logger;

import java.util.Random;

class Cookie
{
    private int counterBits; //用来作为计数器的位数, 总位数等于counterBits + 1
    private int value;
    private int overFlowMask; //用来处理计数器部分的掩码
    /**
     *
     * @param counterBits < 31
     */
    public Cookie(int counterBits) {
        this.counterBits = counterBits;
        this.value = 0;
        this.overFlowMask = (1 << this.counterBits) - 1;
    }

    public boolean isValid()
    {
        return (this.value & (1 << counterBits)) != 0;
    }

    public void setValidBit()
    {
        this.value = (this.value | (1 << this.counterBits));
    }

    public void clearValidBit()
    {
        this.value = (this.value & this.overFlowMask);
    }

    public void increment(int delta)
    {
        this.clearValidBit();
        this.value = (this.value + delta) & this.overFlowMask;
        if(this.value != 0)
        {
            setValidBit();
        }
    }

    public void rightShift(int bits)
    {
        if(bits > counterBits)
        {
            this.value = 0;
            return;
        }
        this.clearValidBit();;
        this.value = (this.value >> bits);
        if(this.value != 0)
        {
            this.setValidBit();
        }
    }
    public int getCounterBits() {
        return counterBits;
    }

    public void setCounterBits(int counterBits) {
        assert counterBits <= Constant.MAX_COOKIE_COUNTER_BITS;
        boolean valid = this.isValid();
        clearValidBit();
        this.counterBits = counterBits;
        this.overFlowMask = (1 << this.counterBits) - 1;
        this.value = (this.value & this.overFlowMask);
        if(valid)
        {
            setValidBit();
        }
        else
        {
            assert this.value == 0;
        }
    }

    //pjy add
    public int getValue() {
        return value;
    }
    //pjy add end
}

class Mask
{
    private int totalBits; //总位数
    private int leftSetBits; //最左s位置为1，注意，s >= 1
    private int mask;
    

    public int getValidBound()//pjy modify 此函数无意义
    {
        return (1 << 1);
    }

    public int getTotalBits() {
        return totalBits;
    }

    public int getMask() {
        return mask;
    }

    public int getLeftSetBits() {
        return leftSetBits;
    }

    public void setLeftSetBits(int leftSetBits) {
        this.leftSetBits = leftSetBits;
        setMask();
    }

    public void leftShiftMask(int bits)
    {
        if(leftSetBits - bits >= 1)
        {
            setLeftSetBits(leftSetBits - bits);
        }
        else
        {
            setLeftSetBits(1);
        }
    }

    public void rightShiftMask(int bits)
    {
        if(leftSetBits + bits <= totalBits)
        {
            setLeftSetBits(leftSetBits + bits);
        }
        else
        {
            setLeftSetBits(totalBits);
        }
    }

    /**
     * 左移一位, 相当于提高要求
     */
    public void leftShiftMask()
    {
        if(leftSetBits <= 1)
            return;
        this.mask = (this.mask ^ (1 << (totalBits - leftSetBits)));
        this.leftSetBits--;
    }

    /**
     * 右移一位，相当于降低要求
     */
    public void rightShiftMask()
    {
        if(leftSetBits >= totalBits)
            return;
        this.leftSetBits++;
        this.mask = (this.mask ^ (1 << (totalBits - leftSetBits)));
    }

    public Mask(int totalBits, int leftSetBits)
    {
        this.totalBits = totalBits;
        this.leftSetBits = leftSetBits;
        setMask();
    }
    private void setMask()
    {
        int t = (1 << leftSetBits) - 1;
        mask = (t << (totalBits - leftSetBits));
    }

    /**
     * 返回 2 ^ (b-1) + 2 ^ (b - s - 1)
     * @return
     */
    public int getThreShold()
    {
        return (1 << (this.totalBits - 1)) + (1 << (this.totalBits - this.leftSetBits));
    }

    /**
     * 返回 2 ^ (b - 1)
     * @return
     */
    public int getThreshold2()
    {
        return (1 << (this.totalBits - 1));
    }
}


public class SuMaxScatterLetRMT extends SuMaxSketch{
    private Random random;
    private Cookie[][] cookie;
    private Mask mask;
    private int packetCounter; //INT包数目
    private int cellCounter;
    private Logger logger = Logger.getLogger("SuMaxScatterLetRMT.class");
    private boolean useSelfAdjust;
    private boolean useMask;
    private double adjustLowBound = 0.1;
    private double adjustUpBound = 0.9;
    private int adjustInterval = 0;

    public SuMaxScatterLetRMT clone()
    {
        return new SuMaxScatterLetRMT(this);
    }
    public Mask getMask() {
        return mask;
    }

    /**
     * 
     * @param rowNum
     * @param bitNum
     */
    public SuMaxScatterLetRMT(int rowNum, int bitNum) {
        super(rowNum, bitNum);
        initRandom();
        System.out.println("This is SuMaxScatterLetRMT");
    }

    public SuMaxScatterLetRMT(SuMaxScatterLetRMT letRMT)
    {
        super(letRMT.rowNum, letRMT.ArrayLengthBit);
        initRandom();
        System.out.println("This is SuMaxScatterLetRMT");
        initCookie(letRMT.cookie[0][0].getCounterBits() + 1);
        if(letRMT.mask != null)
        {
            setMask(new Mask(letRMT.mask.getTotalBits(), letRMT.mask.getLeftSetBits()));
        }
        this.packetCounter = 0;
        this.cellCounter = 0;
        this.useSelfAdjust = letRMT.useSelfAdjust;
        this.adjustLowBound = letRMT.adjustLowBound;
        this.adjustUpBound = letRMT.adjustUpBound;
        this.adjustInterval = letRMT.adjustInterval;
        this.hashes = letRMT.hashes;
    }

    public void setCookie(int cookieTotalBits)
    {
        initCookie(cookieTotalBits);
    }
    public void setMask(Mask mask)
    {
        this.useMask = true;
        this.mask = mask;
    }

    public void setSelfAdjust(double adjustLowBound, double adjustUpBound, int adjustInterval)
    {
        this.useSelfAdjust = true;
        this.adjustLowBound = adjustLowBound;
        this.adjustUpBound = adjustUpBound;
        this.adjustInterval = adjustInterval;
        packetCounter = 0;
        cellCounter = 0;
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
                logger.debug(String.format("Before Insert: Cell[%d][%d]: %x", i, index, cookie[i][index].getValue()));
                cookie[i][index].increment(Constant.INCREMENT_DELTA);
                logger.debug(String.format("After Insert: Cell[%d][%d]: %x", i, index, cookie[i][index].getValue()));
            }
            else
            {
                if(sizeCellArray[i][index] < min)
                {
                    sizeCellArray[i][index] = min;
                    logger.debug(String.format("Before Insert: Cell[%d][%d]: %x", i, index, cookie[i][index].getValue()));
                    cookie[i][index].increment(Constant.INCREMENT_DELTA);
                    logger.debug(String.format("After Insert: Cell[%d][%d]: %x", i, index, cookie[i][index].getValue()));
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
     *
     * @param rowIndex 在哪一行查找
     * @param colAddress 以哪一列为起始地址
     * @param bound 寻找多大的范围，如2 ^ r
     * @return 返回相对偏移
     */
    private int getOffsetByCookieWithMask(int rowIndex, int colAddress, int bound)
    {
        int tmpIndex = 0;
        for(int i = 0; i < bound; i++)
        {
            int colIndex = (colAddress + i) % SUMAX_ARRAY_LENGTH;
            int tmp = mask.getMask() & cookie[rowIndex][colIndex].getValue();
            if(tmp >= mask.getThreShold())
            {
                tmpIndex = i;
                break;
            }
            else if(tmp >= mask.getThreshold2())
            {
                tmpIndex = i;
            }
        }
        int tmpv1 = mask.getMask() & cookie[rowIndex][(colAddress + tmpIndex) % SUMAX_ARRAY_LENGTH].getValue();
        int tmpv2 = mask.getThreShold();
        if(tmpv1 >= tmpv2 && this.useSelfAdjust)
        {
            cellCounter++;
        }
        return tmpIndex;
    }

    private int getOffsetByCookieNoMask(int rowIndex, int colAddress, int bound)
    {
        long max = Long.MIN_VALUE;
        int maxIndex = 0;
        for(int i = 0; i < bound; i++)
        {
            int colIndex = (colAddress + i) % this.SUMAX_ARRAY_LENGTH;
            if((long)(this.cookie[rowIndex][colIndex].getValue()) > max)
            {
                max = this.cookie[rowIndex][colIndex].getValue();
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /**
     * 自适应调节Mask
     */
    private void selfAdjustMask()
    {
        if(this.packetCounter > this.adjustInterval)
        {
            double tmp = (double)this.cellCounter / (double)this.rowNum / (double)this.packetCounter;
            if(tmp < this.adjustLowBound)
            {
                this.mask.rightShiftMask();
                //this.mask.rightShiftMask(2);
            }
            else if(tmp > this.adjustUpBound)
            {
                this.mask.leftShiftMask();
                //this.mask.leftShiftMask(2);
            }
            logger.debug("Mask leftSetBits:" + this.mask.getLeftSetBits());
            this.cellCounter = 0;
            this.packetCounter = 0;
        }
    }



    public Sketchlet getSketchlet(Sketchlet sketchlet)//pjy modify
    {
        if(this.useSelfAdjust)
        {
            this.packetCounter++;
            this.selfAdjustMask();// 每个INT包携带的cell的数目大于阈值或者小于阈值的时候才进行调节
        }
        assert sketchlet instanceof Scatterlet;
        int absAddress = Math.abs(random.nextInt()) % SUMAX_ARRAY_LENGTH;
        Scatterlet scatterlet = (Scatterlet)sketchlet;
        scatterlet.setAddress(absAddress);
        for(int i = 0; i < this.rowNum; i++)
        {
            int offset;
            if(this.useMask)
                offset = getOffsetByCookieWithMask(i, absAddress, Constant.BOUND);
            else
                offset = getOffsetByCookieNoMask(i, absAddress, Constant.BOUND);
            scatterlet.setOffsetByIndex(i, offset);
            scatterlet.setValueByIndex(i, this.sizeCellArray[i][(absAddress + offset) % SUMAX_ARRAY_LENGTH]);
            int colIndex = (absAddress + offset) % SUMAX_ARRAY_LENGTH;
            logger.debug(String.format("Before Taking: Cell[%d][%d]: %x", i, colIndex, this.cookie[i][colIndex].getValue()));
            this.cookie[i][colIndex].rightShift(Constant.RIGHT_SHIFT_BITS);
            logger.debug(String.format("After Taking: Cell[%d][%d]: %x", i, colIndex, this.cookie[i][colIndex].getValue()));
        }
        return scatterlet;
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
}
