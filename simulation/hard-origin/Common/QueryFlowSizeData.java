package Common;

public class QueryFlowSizeData implements QueryData{
    private int[] flowSizeArray;
    private int flowSizeArrayLength;

    /**
     * return all the query cells
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
