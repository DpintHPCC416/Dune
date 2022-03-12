package Common;
//初始化：获得流记录，并进行排序，确定发包顺序
//读文件不应当由此类来执行，读文件产生一个FlowInfo的list放进来是可以的
//按秒循环，按顺序返回一秒内的所有数据包
//发包时间排序精确到毫秒级
//根据flow_info中的pps和持续时间，我们应该得到一个发包的时间序列,然后向一个毫秒级的发包器注册
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class Packet_Generator {
    private int INT_PPS;
    private FlowInfo INT_FlowInfo;
    private ArrayList<FlowInfo> datas;
    private int persist_time;   //持续时间s
    private ArrayList<ArrayList<FlowInfo>> packet_sent;       //发包注册器
    private int time_unit=1000000;      //1秒内有多少个时间片，以us为单位
    private int TotalTimeUnit; 
    public int current_time;       //现在取到了哪一个时间的数据包了
    public Packet_Generator(int INT_PPS,int persist_time,ArrayList<FlowInfo> datas)
    {
        System.out.println("" + time_unit);
        this.current_time = 0;
        this.INT_PPS = INT_PPS;
        this.datas = datas;
        this.persist_time = persist_time;
        this.TotalTimeUnit = this.time_unit*this.persist_time;
        this.packet_sent = new ArrayList<ArrayList<FlowInfo>>(this.TotalTimeUnit);
        //初始化INT流信息
        this.INT_FlowInfo = new FlowInfo();
        this.INT_FlowInfo.pps = this.INT_PPS;
        this.INT_FlowInfo.flowID = new FlowID(0, 0, 0, 0, 0);
        this.INT_FlowInfo.INT = true;
        for(int i=0;i<this.TotalTimeUnit;i++)
            packet_sent.add(new ArrayList<>());
        //注册
        Random random = new Random();
        for(FlowInfo info:this.datas)
        {
            //时间单位为ms
            int realSendNum = 0;
            int time_interval =  (int)Math.round((float) time_unit/(double)info.pps);
            time_interval = (time_interval == 0) ? 1: time_interval;
            //int current_time = random.nextInt(this.TotalTimeUnit);
            int current_time = time_interval;
            //以下设置最大发包数目
            info.realSendNum = 0;
            while(current_time < this.TotalTimeUnit)
            {
                packet_sent.get(current_time).add(info);
                realSendNum++;
                current_time += time_interval;
            }
            info.realSendNum = realSendNum;
        }
        //注册INT Flow
        int time_interval =  (int)Math.round((float) time_unit/(double)this.INT_PPS);
        time_interval = (time_interval == 0)? 1 : time_interval;
        int current_time = time_interval;
        while(current_time < this.TotalTimeUnit)
        {
            packet_sent.get(current_time).add(INT_FlowInfo);
            current_time += time_interval;
        }
        //打乱顺序:Knuth-Durstenfeld Shuffle洗牌算法 
        //从前面的k各种随机选一个交换到尾部
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

    public ArrayList<FlowInfo> GetInfos()     //返回当前毫秒的所有数据包   
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
