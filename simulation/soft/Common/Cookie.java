package Common;

public class Cookie
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

