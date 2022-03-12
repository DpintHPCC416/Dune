package Common;

public class Mask
{
    private int totalBits; //总位数
    private int leftSetBits; //最左s位置为1，注意，s >= 1
    private int mask;




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
    public int getThreshold()
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
