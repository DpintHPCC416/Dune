package Common;

import java.util.concurrent.CountDownLatch;

public class Constant {


    //BOBHASH32
    public static final int MAX_PRIME32 = 1229;


    //SuMAX Sketch
    public static final int SUMAX_ARRAYS_NUM = 2; //列数
    public static final int SUMAX_ARRAY_BITNUM = 15;



    public static final int PROTOCOL_TCP = 6;


    //SuMaxScatterLet
    public static int BOUND = 64;

    //Debug
    public static final Boolean DEBUG_FLAG = false;

    //packet generator
    public static final int TIME_UNIT = 1000000;

    //Cookie
    public static final int MAX_COOKIE_COUNTER_BITS = 30;

    public static final int INCREMENT_DELTA = 1;

    //pjy add end

    //FIFO checking thread
    public static final int FIFO_THREAD_STEP = 2;
    public static final int FIFO_PACKET_STEP = 10; //每10个包处理一次
    public static final int SELF_ADJUSTING_INTERVAL = 10000; //每10ms自适应调整一次
    public static final int BUFFER_SIZE = 10000;
}
