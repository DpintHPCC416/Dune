package Common;

public class QueryFlowSizeData implements QueryData{
    private int[] flowSizeArray;
    private int flowSizeArrayLength;

    /**
     * 需要把所有查询的cell的值都返回
     * @param num
     */
    public QueryFlowSizeData(int num)
    {
        this.flowSizeArray = new int[num];
        this.flowSizeArrayLength = num;
    }

    public void setFlowSize(int index, int value)
    {
        if(index < this.flowSizeArrayLength)
            flowSizeArray[index] = value;
    }

    public int[] getFlowSize()
    {
        return this.flowSizeArray;
    }
}
