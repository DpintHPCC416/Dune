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
import java.util.List;

/**
 * 专门用于SimulationExSoft结构的仿真程序
 */
public class SimulationExSoft {
    public static void main(String[] args) {
        ArgumentParser parser = ArgumentParsers.newFor("Main").build().defaultHelp(true);
        parser.addArgument("--fin").help("data file path");
        parser.addArgument("--fout").help("result file path");
        parser.addArgument("--sketch").choices("ScatterSoft").help("the sketch to be used");
        parser.addArgument("--flowNum").type(Integer.class).help("simulation flow num");
        parser.addArgument("--dtime").type(Integer.class).help("persist time(s)");
        parser.addArgument("--pps").type(Integer.class).help("INT flow pps");
        parser.addArgument("-m", "--mask").type(Integer.class).nargs(2).help("mask arguments: total_bits left_set_bits");
        parser.addArgument("-a", "--adjust").type(Integer.class).nargs(2).help("self adjusting arguments: low_bound up_bound");
        parser.addArgument("-c", "--cookie").type(Integer.class).help("Cookie total bits");
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
        if(ns.get("boundBits") != null)
        {
            Constant.BOUND = (1 << ns.getInt("boundBits"));
        }
        else
        {
            Constant.BOUND = 64;
        }
        System.out.println("Bound: " + Constant.BOUND);
        FactorySketch factory;
        if (sketch_name.trim().equals("ScatterSoft"))
        {
            FactoryScatterSketchSoft sketchSoft = new FactoryScatterSketchSoft();
            sketchSoft.setSketch(Constant.SUMAX_ARRAYS_NUM, Constant.SUMAX_ARRAY_BITNUM);
            if(ns.get("cookie") != null)
            {
                sketchSoft.setCookie(ns.getInt("cookie"));
            }
            else
            {
                System.exit(-1);
            }
            if(ns.get("mask") != null)
            {
                List<Integer> maskArgs = ns.getList("mask");
                sketchSoft.setMask(maskArgs.get(0), maskArgs.get(1));
            }
            else
            {
                System.exit(-1);
            }

            factory = sketchSoft;
        }
        else
        {
            System.out.println("Sketch name must be ScatterSoft");
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
        //使用相同的hash函数
        SuMaxSketch sketch = factory.getSuMaxSketch();
        sketch.setHashes(hashes);
        List<Integer> adjustArgs = null;
        if(ns.get("adjust") != null)
        {
            adjustArgs = ns.getList("adjust");
        }
        else
        {
            System.exit(-1);
        }
        SoftSwitch simSwitch = new SoftSwitch(sketch, adjustArgs.get(0), adjustArgs.get(1));

        sketch = factory.getSuMaxSketch();
        sketch.setHashes(hashes);
        SoftReceiver receiver = new SoftReceiver(sketch);

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
                simSwitch.process(generator.current_time, packet);
                receiver.Receive_Packet(packet, simSwitch);
            }
        }
        System.out.println("最后的mask是:" + (((SuMaxScatterLetSoft)(simSwitch.getSketch())).getMask()));
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
                writer.write("," + (info.flowID.getHashValues()[0] % sketch.getSUMAX_ARRAY_LENGTH()));
                writer.write("," + (info.flowID.getHashValues()[1] % sketch.getSUMAX_ARRAY_LENGTH()));
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
