package Utility;

import Common.Constant;
import Common.FlowID;
import Common.FlowInfo;
import Sketch.SuMaxSketch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Util {

    public static byte[] long2Bytes(long value) {
        byte[] b = new byte[8];
        for (int i = 0; i < 8; i++) {
            b[8 - i - 1] = (byte)((value >> 8 * i) & 0xff);
        }
        return b;
    }

    public static List<Integer> getRandomList(int total, int needed)
    {
        Set<Integer> set = new HashSet<>();
        Random random = new Random();
        long seed = System.currentTimeMillis();
        if(Constant.DEBUG_FLAG)
            seed = 1;
        random.setSeed(seed);
        int num = 0;
        while (num < needed)
        {
            int tmp = Math.abs(random.nextInt()) % total;
            while (set.contains(tmp))
            {
                tmp = Math.abs(random.nextInt()) % total;
            }
            set.add(tmp);
            num++;
        }
        List<Integer> subList = new ArrayList<>(set);
        Collections.sort(subList);
        return subList;
    }


    /**
     * 从line中解析除流的信息
     * line必须满足的格式为
     * 源ip，源端口，目的ip，目的端口，pps，在trace中的发包数
     * @param line
     * @return
     */
    public static FlowInfo parseFlowInfoFromLine(String line)
    {
        String[] args = line.trim().split(",");
        FlowInfo flowInfo = new FlowInfo();
        long srcIP = Long.parseLong(args[0]);
        long dstIP = Long.parseLong(args[2]);
        int srcPort = Integer.parseInt(args[1]);
        int dstPort = Integer.parseInt(args[3]);
        double pps = Double.parseDouble(args[4]);
        pps = (pps > 0) ? pps : 1;
        int packetNum = Integer.parseInt(args[5]);
        flowInfo.flowID = new FlowID(srcIP, dstIP, srcPort, dstPort, Constant.PROTOCOL_TCP);
        flowInfo.packetNum = packetNum;
        flowInfo.pps = pps;
        flowInfo.INT = false;
        return flowInfo;
    }

    public static void printSketch(SuMaxSketch sketch, String sketchFileName)
    {
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(sketchFileName));
            int[][] cellArray = sketch.getSizeCellArray();
            for(int i = 0; i < sketch.getRowNum(); i++)
            {
                bufferedWriter.write("line " + i + "\n");
                for(int j = 0; j < sketch.getSUMAX_ARRAY_LENGTH(); j++)
                {
                    if(j % 16 == 0 && j != 0)
                    {
                        bufferedWriter.write("\n");
                    }
                    bufferedWriter.write(" " + cellArray[i][j]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}