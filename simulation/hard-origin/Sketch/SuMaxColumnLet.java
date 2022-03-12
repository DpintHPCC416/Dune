package Sketch;

import Common.Constant;

import java.util.Random;

class Columnlet implements Sketchlet{
    private int address;
    private int[] values;
    public Columnlet(int rowNum)
    {
        this.values = new int[rowNum];
    }

    public int getAddress()
    {
        return this.address;
    }

    public int getValueByIndex(int index)
    {
        return this.values[index];
    }

    public void setAddress(int address)
    {
        this.address = address;
    }

    public void setValueByIndex(int index, int value)
    {
        this.values[index] = value;
    }
}


public class SuMaxColumnLet extends SuMaxSketch{
    private boolean[][] kPlusArray;
    private int kPlusArrayRowNum;
    private int kPlusArrayColNum;
    private Random random;
    public SuMaxColumnLet(int rowNum, int bitNum) {
        super(rowNum, bitNum);
        initkPlusArray(Constant.KPLUS_K);
        initRandom();
        System.out.println("This is SuMaxColumnLet");
    }

    public SuMaxColumnLet(int rowNum, int bitNum, double theda) {
        super(rowNum, bitNum, theda);
        initRandom();
        initkPlusArray(Constant.KPLUS_K);
    }

    private void initRandom()
    {
        long seed = System.currentTimeMillis();
        if(Constant.DEBUG_FLAG)
            seed = 1;
        random = new Random(seed);
    }

    public void initkPlusArray(int k)
    {
        this.kPlusArray = new boolean[k][SUMAX_ARRAY_LENGTH];
        this.kPlusArrayRowNum = k;
        this.kPlusArrayColNum = this.SUMAX_ARRAY_LENGTH;
    }

    public void clearKPlusArray()
    {
        for(int i = 0; i < kPlusArrayRowNum; i++)
        {
            for(int j = 0; j < kPlusArrayColNum; j++)
            {
                kPlusArray[i][j] = false;
            }
        }
    }

    private int getColIndexByKPlus()
    {
        for(int i = 0; i < kPlusArrayRowNum; i++)
        {
            int index = Math.abs(random.nextInt()) % this.kPlusArrayColNum;
            if(this.kPlusArray[i][index] == false)
            {
                this.kPlusArray[i][index] = true;
                return index;
            }
        }
        return Math.abs(random.nextInt()) % this.kPlusArrayColNum;
    }

    public Sketchlet getSketchlet(Sketchlet sketchlet)
    {
        assert sketchlet instanceof Columnlet;
        Columnlet columnlet = (Columnlet) sketchlet;
        int colIndex = getColIndexByKPlus();
        columnlet.setAddress(colIndex);
        for(int i = 0; i < this.rowNum; i++)
        {
            columnlet.setValueByIndex(i, this.sizeCellArray[i][colIndex]);
        }
        return columnlet;
    }

    /**
     * 通过sketchlet更新sketch，只在终端进行，所以没有更新k+chance
     * @param sketchlet
     */
    public void setBySketchlet(Sketchlet sketchlet)
    {
        assert sketchlet instanceof SuMaxColumnLet;
        Columnlet columnlet = (Columnlet) sketchlet;
        int colIndex = columnlet.getAddress();
        for(int i = 0; i < this.rowNum; i++)
        {
            this.sizeCellArray[i][colIndex] = columnlet.getValueByIndex(i);
        }
    }


    public void handleSizeArray(int value)
    {
        int min = Integer.MAX_VALUE;
        for(int i = 0; i < this.rowNum; i++)
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
        }
    }
}
