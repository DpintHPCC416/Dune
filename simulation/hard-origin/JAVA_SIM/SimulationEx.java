package JAVA_SIM;

import Common.*;
import Sketch.*;
import Utility.Hash;
import Utility.Md5;
import Utility.Sha256;
import Utility.Util;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SimulationEx {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build().defaultHelp(true);
        parser.addArgument("--fin").help("data file path");
        parser.addArgument("--fout").help("result file path");
        parser.addArgument("--sketch").choices("Column", "ScatterBitmap", "ScatterRMT").help("the sketch to be used");
        parser.addArgument("--flowNum").type(Integer.class).help("simulation flow num");
        parser.addArgument("--dtime").type(Integer.class).help("persist time(s)");
        parser.addArgument("--pps").type(Integer.class).help("INT flow pps");
        parser.addArgument("-m", "--mask").type(Integer.class).nargs(2).help("mask arguments: total_bits left_set_bits");
        parser.addArgument("-a", "--adjust").type(Double.class).nargs(2).help("self adjusting arguments: low_bound up_bound");
        parser.addArgument("-c", "--cookie").type(Integer.class).help("Cookie total bits");
        parser.addArgument("-I", "--interval").type(Integer.class).help("Adjusting interval");
        parser.addArgument("--boundBits").type(Integer.class).help("bound bits, e.x., bits=6, bound = 2 ^ 6 = 64");
        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        String in_file_name = ns.getString("fin");
        String out_file_name = ns.getString("fout");
        String sketch_name = ns.getString("sketch");
        int simulate_flow_num = ns.getInt("flowNum");
        int persist_time = ns.getInt("dtime");
        int INT_pps = ns.getInt("pps");
        FactorySketch factory;
        if(ns.get("boundBits") != null)
        {
            Constant.BOUND = (1 << ns.getInt("boundBits"));
        }
        else
        {
            Constant.BOUND = 64;
        }
        System.out.println("Bound: " + Constant.BOUND);
        if(sketch_name.trim().equals("Column"))
        {
            factory = new FactoryColumnSketch();
        }
        else if(sketch_name.trim().equals("ScatterBitmap"))
        {
            factory = new FactoryScatterSketch();
        }
        else if (sketch_name.trim().equals("ScatterRMT"))
        {
            FactoryScatterSketchRMT sketchRMT = new FactoryScatterSketchRMT();
            sketchRMT.setSketch(Constant.SUMAX_ARRAYS_NUM, Constant.SUMAX_ARRAY_BITNUM);
            if(ns.get("cookie") != null)
            {
                sketchRMT.setCookie(ns.getInt("cookie"));
            }
            if(ns.get("mask") != null)
            {
                List<Integer> maskArgs = ns.getList("mask");
                sketchRMT.setMask(maskArgs.get(0), maskArgs.get(1));
            }
            if(ns.get("adjust") != null)
            {
                List<Double> adjustArgs = ns.getList("adjust");
                int adjustInterval = Constant.UPDATE_NUMBER;
                if(ns.get("interval") != null)
                    adjustInterval = ns.getInt("interval");
                sketchRMT.setSelfAdjust(adjustArgs.get(0), adjustArgs.get(1), adjustInterval);
            }
            factory = sketchRMT;
        }
        else
        {
            System.out.println("Sketch name must be Column or Scatter_bitmap or Scatter_RMT");
            return;
        }
        Hash[] hashes = new Hash[Constant.SUMAX_ARRAYS_NUM];
        hashes[0] = new Md5();
        if(Constant.DEBUG_FLAG)
        {
            byte[] seed = Util.long2Bytes(1);
            hashes[0].setSeed(seed);
        }
        hashes[1] = new Sha256();
        if(Constant.DEBUG_FLAG)
        {
            byte[] seed = Util.long2Bytes(1);
            hashes[1].setSeed(seed);
        }
        //use the same hash functions
        SuMaxSketch sketch = factory.getSuMaxSketch();
        sketch.setHashes(hashes);
        Switch simSwitch = new Switch(sketch);

        sketch = factory.getSuMaxSketch();
        sketch.setHashes(hashes);
        Receiver receiver = new Receiver(sketch);

        //生成sketchlet，复用
        Sketchlet sketchlet = factory.getSketchLet();

        ArrayList<FlowInfo> datas = new ArrayList<>(simulate_flow_num);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(in_file_name));
            List<Integer> simList = null;
            int totalFlowNum = Integer.parseInt(reader.readLine());
            simList = Util.getRandomList(totalFlowNum, simulate_flow_num);
            assert simList.size() == simulate_flow_num;

            int currentLine = 0;
            int curPos = 0;
            int nextParseLine = simList.get(curPos);

            do {
                String line = reader.readLine();
                if(line == null)
                    break;
                if(currentLine == nextParseLine)
                {
                    datas.add(Util.parseFlowInfoFromLine(line));
                    curPos++;
                    if(curPos >= simulate_flow_num)
                        break;
                    nextParseLine = simList.get(curPos);
                }
                currentLine++;
            }while (true);
            assert datas.size() == simulate_flow_num;
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //为每个流提前计算好各行的hash值
        for(FlowInfo info: datas)
        {
            info.flowID.setHashValues(hashes);
        }
        Packet_Generator generator = new Packet_Generator(INT_pps, persist_time, datas);
        Packet packet = new Packet(false);
        ArrayList<FlowInfo> infos;
        while ((infos = generator.GetInfos()) != null)
        {
            int packetNum = infos.size();
            for(int i = 0; i < packetNum; i++)
            {
                packet.packetClear();
                FlowInfo flowInfo = infos.get(i);
                packet.packetSet(flowInfo, 0, flowInfo.INT);
                simSwitch.process(packet, sketchlet);
                receiver.Receive_Packet(packet);
            }
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(out_file_name));
            SuMaxSketch receiverSketch = receiver.getSketch();
            for(FlowInfo info: datas)
            {
                writer.write(""
                        + info.flowID.srcIP + ","
                        + info.flowID.srcPort + ","
                        + info.flowID.dstIP + ","
                        + info.flowID.dstPort + ","
                        + (int)info.pps + ","
                        + info.realSendNum
                );
                QueryData result = simSwitch.querySketch(info.flowID, QueryType.QUERY_SIZE);
                if(result instanceof QueryFlowSizeData)
                {
                    for(int flowSize: ((QueryFlowSizeData)result).getFlowSize())
                    {
                        writer.write("," + flowSize);
                    }
                }
                result = receiverSketch.query(info.flowID, QueryType.QUERY_SIZE);
                if(result instanceof QueryFlowSizeData)
                {
                    for(int flowSize: ((QueryFlowSizeData)result).getFlowSize())
                    {
                        writer.write("," + flowSize);
                    }
                }
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
