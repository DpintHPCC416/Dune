package Common;
/**
 * initialization: get flow record and sort them to determine sending sequences
 * according to pps and persist-time in flow_info, we could get a time-sequence of sending packets
 * and register them to millisecond-level packet generator
*/
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Packet_Generator {
    private int INT_PPS;
    private FlowInfo INT_FlowInfo;
    private ArrayList<FlowInfo> datas;
    private int persist_time;   
    private ArrayList<ArrayList<FlowInfo>> packet_sent;       //packet generator
    private int time_unit=1000000;      //how much time slice in 1s, unit in ms
    private int TotalTimeUnit; 
    private int current_time;       // which packet has been caught in this time
    public Packet_Generator(int INT_PPS,int persist_time,ArrayList<FlowInfo> datas)
    {
        System.out.println("" + time_unit);
        this.current_time = 0;
        this.INT_PPS = INT_PPS;
        this.datas = datas;
        this.persist_time = persist_time;
        this.TotalTimeUnit = this.time_unit*this.persist_time;
        this.packet_sent = new ArrayList<ArrayList<FlowInfo>>(this.TotalTimeUnit);
        //initialize INT flow info
        this.INT_FlowInfo = new FlowInfo();
        this.INT_FlowInfo.pps = this.INT_PPS;
        this.INT_FlowInfo.flowID = new FlowID(0, 0, 0, 0, 0);
        this.INT_FlowInfo.INT = true;
        for(int i=0;i<this.TotalTimeUnit;i++)
            packet_sent.add(new ArrayList<>());
        //register
        Random random = new Random();
        for(FlowInfo info:this.datas)
        {
            int realSendNum = 0;
            int time_interval =  (int)Math.round((float) time_unit/(double)info.pps);
            time_interval = (time_interval == 0) ? 1: time_interval;
            //int current_time = random.nextInt(this.TotalTimeUnit);
            int current_time = time_interval;
            info.realSendNum = 0;
            while(current_time < this.TotalTimeUnit)
            {
                packet_sent.get(current_time).add(info);
                realSendNum++;
                current_time += time_interval;
            }
            info.realSendNum = realSendNum;
        }
        //register INT Flow
        int time_interval =  (int)Math.round((float) time_unit/(double)this.INT_PPS);
        time_interval = (time_interval == 0)? 1 : time_interval;
        int current_time = time_interval;
        while(current_time < this.TotalTimeUnit)
        {
            packet_sent.get(current_time).add(INT_FlowInfo);
            current_time += time_interval;
        }
        if(!Constant.DEBUG_FLAG)
        {
            for(ArrayList<FlowInfo> Pakcet_Array:packet_sent)
            {
                if(Pakcet_Array.size() >1)
                {
                    Collections.shuffle(Pakcet_Array);
                }
            }
        }
    }

    public ArrayList<FlowInfo> GetInfos()     //return all the packets in current ms 
    {
        if(current_time>=TotalTimeUnit)
            return null;
        ArrayList<FlowInfo> Info_Array;
        do{
            Info_Array = packet_sent.get(current_time);
            current_time++;
        }while(Info_Array.size() ==0 && current_time<TotalTimeUnit);
        if(Info_Array.size()!= 0)
            return Info_Array;
        else 
            return null;
    }   

}
