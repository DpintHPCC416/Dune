package Sketch;

import Common.Constant;

import java.util.Random;

class Scatterlet implements Sketchlet{
    private int address;
    private int[] offsetes;
    private int[] values;

    /**
     * 第一个为绝对地址，后面的为相对的地址
     * @param rowNum 每个sketchlet携带多少行的数据
     */
    public Scatterlet(int rowNum)
    {
        this.offsetes = new int[rowNum];
        this.values = new int[rowNum];
    }

    public void setAddress(int address)
    {
        this.address = address;
    }

    public int getAddress()
    {
        return address;
    }
    public int getOffsetByIndex(int index)
    {
        return this.offsetes[index];
    }

    public int getValueByIndex(int index)
    {
        return this.values[index];
    }

    public void setOffsetByIndex(int index, int address)
    {
        this.offsetes[index] = address;
    }

    public void setValueByIndex(int index, int value)
    {
        this.values[index] = value;
    }
}




public class SuMaxScatterLet extends SuMaxSketch{
    private boolean[][] bitmap;
    private Random random;
    public SuMaxScatterLet(int rowNum, int bitNum) {

        super(rowNum, bitNum);
        initRandom();
        initBitMapArray();
        System.out.println("This is SuMaxScatterLet");
    }

    public SuMaxScatterLet(int rowNum, int bitNum, double theda) {
        super(rowNum, bitNum, theda);
        initRandom();
        initBitMapArray();
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
            }
            else
            {
                if(sizeCellArray[i][index] < min)
                {
                    sizeCellArray[i][index] = min;
                }
            }
            //多了这行
            this.bitmap[i][index] = true;
        }
    }

    private void initRandom()
    {
        long seed = System.currentTimeMillis();
        if(Constant.DEBUG_FLAG)
            seed = 1;
        random = new Random(seed);
    }
    public void initBitMapArray()
    {
        this.bitmap = new boolean[this.rowNum][this.SUMAX_ARRAY_LENGTH];
    }

    public void clearBitMapArray()
    {
        for(int i = 0; i < rowNum; i++)
        {
            for(int j = 0; j < SUMAX_ARRAY_LENGTH; j++)
            {
                this.bitmap[i][j] = false;
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
    private int getOffsetByBitMap(int rowIndex, int colAddress, int bound)
    {
        for(int i = 0; i < bound; i++)
        {
            int colIndex = (colAddress + i) % SUMAX_ARRAY_LENGTH;
            if(bitmap[rowIndex][colIndex] == true)
                return i;
        }
        return Math.abs(random.nextInt()) % bound;
    }

    public void handleSizeArray(byte[] idBytes, int value)
    {

    }

    public Sketchlet getSketchlet(Sketchlet sketchlet)//在这个函数中实现算法2,复制此文件并实现算法3
    {
        assert sketchlet instanceof Scatterlet;
        int absAddress = Math.abs(random.nextInt()) % SUMAX_ARRAY_LENGTH;
        Scatterlet scatterlet = (Scatterlet)sketchlet;
        scatterlet.setAddress(absAddress);
        for(int i = 0; i < this.rowNum; i++)
        {
            int offset = getOffsetByBitMap(i, absAddress, Constant.BOUND);
            scatterlet.setOffsetByIndex(i, offset);
            scatterlet.setValueByIndex(i, this.sizeCellArray[i][(absAddress + offset) % SUMAX_ARRAY_LENGTH]);
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
