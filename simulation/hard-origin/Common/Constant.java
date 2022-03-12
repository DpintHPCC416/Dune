package Common;

import java.io.BufferedWriter;
import java.util.concurrent.CountDownLatch;

public class Constant {

    public static BufferedWriter MASK_WRITER = null;
    //Not constant
    public static final CountDownLatch latch = new CountDownLatch(1);

    //Cell type
    public static final int CELL_SIZE = 0;

    //BOBHASH32
    public static final int MAX_PRIME32 = 1229;


    //SuMAX Sketch
    public static final int SUMAX_ARRAYS_NUM = 2; //column number
    public static final int SUMAX_HASH_FUN_NUM = 2;
    public static final int SUMAX_ARRAY_BITNUM = 15;


    //INT flow
    public static final int INT_PPS = 1000;

    //Protocol
    public static final int PROTOCOL_TCP = 6;

    //SuMaxColumnLet
    public static final int KPLUS_K = 8;

    //SuMaxScatterLet
    public static int BOUND = 64;

    //Debug
    public static final Boolean DEBUG_FLAG = false;

    //packet generator
    public static final int TIME_UNIT = 1000000;

    //Cookie
    public static final int MAX_COOKIE_COUNTER_BITS = 30;
    //pjy add
    public static final int COOKIE_COUNTER_BITS = 29;
    public static final int RIGHT_SHIFT_BITS = 2;
    public static final int INCREMENT_DELTA = 1;
    //pjy add end

    //pjy add
    //Mask
    public static final int INIT_TOTALBITS = 30;
    public static final int INIT_LEFTSETBITS = 10;
    public static final int UPDATE_NUMBER = 1000;//检查更新mask的leftSetBits的阈值
    public static final double THRESHOLD_PLUS_CPU = 0.1;
    public static final double THRESHOLD_INCREASE_CPU = 0.9;
    //pjy add end


}
