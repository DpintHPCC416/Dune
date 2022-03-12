package Sketch;

/**
 * Only used for soft Scatter
 */

import Common.*;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;

public class SoftSwitch{
    private Logger logger = Logger.getLogger("SoftSwitch.class");
    private SuMaxSketch sketch;
    private TelemetryData data;
    private LinkedList<Scatterlet> FIFO;
    private LinkedList<Scatterlet> bufferScatterLet;
    private Scatterlet tmplet;
    private int curCol;
    private int curTime;
    private int curTimePacketProcessed; //在这个时刻已经处理的包数目
    private int lowBound;
    private int upBound;
    public SoftSwitch(SuMaxSketch sketch, int lowBound, int upBound) {
        this.sketch = sketch;
        this.data = new TelemetryData();
        FIFO = new LinkedList<>();
        bufferScatterLet = new LinkedList<>();
        initBuffer();
        tmplet = new Scatterlet(Constant.SUMAX_ARRAYS_NUM);
        this.curTime = 0;
        this.curTimePacketProcessed = 0;
        this.lowBound = lowBound;
        this.upBound = upBound;
    }

    public SuMaxSketch getSketch() {
        return sketch;
    }

    public QueryData querySketch(FlowID id, QueryType type)
    {
        return sketch.query(id, type);
    }

    private void initBuffer()
    {
        for(int i = 0; i < Constant.BUFFER_SIZE; i++)
        {
            this.bufferScatterLet.add(new Scatterlet(Constant.SUMAX_ARRAYS_NUM));
        }
    }

    public void addToBuffer(Sketchlet scatterlet)
    {
        this.bufferScatterLet.add((Scatterlet) scatterlet);
    }
    /**
     * 根据时间进行软件线程模拟
     * @param time
     * @param packet
     * @return
     */
    public boolean process(int time, Packet packet)
    {
        if(time != this.curTime)
        {
            for(int i = curTime; i < time; i++)
            {
                handleFIFO();
                if(i != 0 && i % Constant.SELF_ADJUSTING_INTERVAL == 0)
                {
                    selfAdjust();
                }
            }
            this.curTime = time;
            this.curTimePacketProcessed = 0;
        }
        if((curTimePacketProcessed != 0) && (curTimePacketProcessed % Constant.FIFO_PACKET_STEP == 0))
        {
            handleFIFO();
        }
        data.packetSize = 1;
        curTimePacketProcessed++;
        if(packet.INT == false)
        {
            sketch.Insert(packet, data); //不是INT流才插入数据
        }
        if(packet.INT == true)
        {
            if(FIFO.size() > 0)
            {
                Scatterlet tmpLet = FIFO.removeFirst();
                int absAddress = tmpLet.getAddress();
                for(int i = 0; i < Constant.SUMAX_ARRAYS_NUM; i++)
                {
                    tmpLet.setValueByIndex(i, sketch.getValue(i, (absAddress + tmpLet.getOffsetByIndex(i)) % sketch.SUMAX_ARRAY_LENGTH));
                }
                logger.debug(String.format("Taking from FIFO abs address0 address1 : %d %d %d", absAddress ,absAddress+ tmpLet.getOffsetByIndex(0), absAddress + tmpLet.getOffsetByIndex(1)));
                packet.sketchlet = tmpLet;
            }
            else
            {
                packet.sketchlet = null;
            }
        }
        return true;
    }

    public void handleFIFO()
    {
        SuMaxScatterLetSoft letSoft = (SuMaxScatterLetSoft)(this.sketch);
        //每次模拟前进这么多步
        for(int i = 0; i < Constant.FIFO_THREAD_STEP; i++)
        {
            boolean hasFound = false;
            tmplet.clear();
            for(int j = 0; j < Constant.SUMAX_ARRAYS_NUM; j++)
            {
                //说明找到
                if(letSoft.getOffsetByCookie(j, this.curCol, Constant.BOUND, tmplet))
                {
                    hasFound = true;
                }
                //添加这里，只有找到两个大于阈值的值才加入
                else
                {
                    hasFound = false;
                }
            }
            if(hasFound)
            {
                tmplet.isValid = true;
                tmplet.setAddress(this.curCol);
                if(bufferScatterLet.size() != 0)
                {
                    Scatterlet scatterlet = bufferScatterLet.removeFirst();
                    scatterlet.copyOf(tmplet);
                    FIFO.add(scatterlet);
                    SuMaxScatterLetSoft curSketch = (SuMaxScatterLetSoft) (this.sketch);
                    int absAddress = scatterlet.getAddress();
                    //更新cookie
                    for(int j = 0; j < Constant.SUMAX_ARRAYS_NUM; j++)
                    {
                        curSketch.cookie[j][(absAddress + scatterlet.getOffsetByIndex(j)) % curSketch.SUMAX_ARRAY_LENGTH].rightShift(1);
                    }
                    logger.info(String.format("Add to FIFO abs + offset: %d %d %d ",absAddress,  absAddress + scatterlet.getOffsetByIndex(0), absAddress + scatterlet.getOffsetByIndex(1)));
                }
                else
                {
                    FIFO.add(tmplet.clone());
                }

                int min = tmplet.getOffsetByIndex(0);
                for(int j = 0; j < Constant.SUMAX_ARRAYS_NUM; j++)
                {
                    if(tmplet.getOffsetByIndex(j) < min)
                    {
                        min = tmplet.getOffsetByIndex(j);
                    }
                }
                this.curCol = (this.curCol + 1) % letSoft.SUMAX_ARRAY_LENGTH;
            }
            else
            {
                this.curCol = (this.curCol + Constant.BOUND) % letSoft.SUMAX_ARRAY_LENGTH;
            }
        }
    }

    public void selfAdjust()
    {
        SuMaxScatterLetSoft scatterLetSoft = ((SuMaxScatterLetSoft) (this.sketch));
        if(FIFO.size() < this.lowBound)
        {
            scatterLetSoft.getMask().rightShiftMask();
            logger.debug("after adjusting mask: " + scatterLetSoft.getMask().getLeftSetBits() + " " + FIFO.size());
        }
        else if(FIFO.size() > this.upBound)
        {
            scatterLetSoft.getMask().leftShiftMask();
            logger.debug("after adjusting mask: " + scatterLetSoft.getMask().getLeftSetBits() + " " + FIFO.size());
        }
    }
}
