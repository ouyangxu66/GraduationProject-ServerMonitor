package com.xu.monitorcommon.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.moudule.ProcessModel;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控工具类 (Full Version)
 * 包含：基础信息、CPU/内存/磁盘/网络、磁盘IO速率、Top5进程
 */
public class SystemMonitorUtil {

    private static final SystemInfo SI = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    private static final OperatingSystem OS = SI.getOperatingSystem();
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("#.00");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static BaseMonitorModel collect() throws InterruptedException {
        BaseMonitorModel model = new BaseMonitorModel();

        // 1. 基础信息
        model.setOsName(OS.toString());
        model.setHostName(OS.getNetworkParams().getHostName());
        model.setIp(getLocalIp());
        model.setUpTime(OS.getSystemUptime());

        // 2. 内存信息
        GlobalMemory memory = HAL.getMemory();
        double totalMem = memory.getTotal() / 1024.0 / 1024.0 / 1024.0;
        double usedMem = (memory.getTotal() - memory.getAvailable()) / 1024.0 / 1024.0 / 1024.0;
        model.setMemoryTotal(parse(totalMem));
        model.setMemoryUsed(parse(usedMem));

        // 3. 磁盘容量信息
        FileSystem fileSystem = OS.getFileSystem();
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        long totalDiskBytes = 0;
        long usedDiskBytes = 0;
        for (OSFileStore fs : fileStores) {
            totalDiskBytes += fs.getTotalSpace();
            usedDiskBytes += (fs.getTotalSpace() - fs.getUsableSpace());
        }
        double totalDiskGb = totalDiskBytes / 1024.0 / 1024.0 / 1024.0;
        double usedDiskGb = usedDiskBytes / 1024.0 / 1024.0 / 1024.0;
        model.setDiskTotal(parse(totalDiskGb));
        model.setDiskUsed(parse(usedDiskGb));
        model.setDiskUsage(totalDiskGb > 0 ? parse((usedDiskGb / totalDiskGb) * 100) : 0);

        // ==========================================
        // 4. 动态速率采样 (CPU, Net, Disk IO)
        // ==========================================
        CentralProcessor processor = HAL.getProcessor();
        List<NetworkIF> networkIFs = HAL.getNetworkIFs();
        List<HWDiskStore> diskStores = HAL.getDiskStores(); // 获取磁盘列表

        // --- 📸 第一次采样 ---
        long[] prevCpuTicks = processor.getSystemCpuLoadTicks();

        long prevNetRecv = 0;
        long prevNetSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            prevNetRecv += net.getBytesRecv();
            prevNetSent += net.getBytesSent();
        }

        // 磁盘 IO 第一次采样
        long prevDiskRead = 0;
        long prevDiskWrite = 0;
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes(); // 必须更新
            prevDiskRead += disk.getReadBytes();
            prevDiskWrite += disk.getWriteBytes();
        }

        //休眠 1 秒
        TimeUnit.SECONDS.sleep(1);

        // --- 📸 第二次采样 ---
        long[] currCpuTicks = processor.getSystemCpuLoadTicks();

        long currNetRecv = 0;
        long currNetSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            currNetRecv += net.getBytesRecv();
            currNetSent += net.getBytesSent();
        }

        // 磁盘 IO 第二次采样
        long currDiskRead = 0;
        long currDiskWrite = 0;
        for (HWDiskStore disk : diskStores) {
            disk.updateAttributes();
            currDiskRead += disk.getReadBytes();
            currDiskWrite += disk.getWriteBytes();
        }

        // --- 计算并赋值 ---
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        model.setCpuLoad(parse(cpuLoad));

        model.setNetRecvRate(parse((currNetRecv - prevNetRecv) / 1024.0));
        model.setNetSentRate(parse((currNetSent - prevNetSent) / 1024.0));

        // 🟢 赋值磁盘速率
        model.setDiskReadRate(parse((currDiskRead - prevDiskRead) / 1024.0));
        model.setDiskWriteRate(parse((currDiskWrite - prevDiskWrite) / 1024.0));

        // 5. 系统负载
        double[] loads = HAL.getProcessor().getSystemLoadAverage(3);
        model.setSystemLoad1(formatLoad(loads[0]));
        model.setSystemLoad5(formatLoad(loads[1]));
        model.setSystemLoad15(formatLoad(loads[2]));


        // 6. 获取 Top 5 进程
        List<ProcessModel> processList = new ArrayList<>();
        // 按 CPU 降序，取前 5
        List<OSProcess> osProcesses = OS.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 5);

        for (OSProcess p : osProcesses) {
            ProcessModel pm = new ProcessModel();
            pm.setName(p.getName());
            pm.setPid(p.getProcessID());

            long procUptime = p.getUpTime();
            if (procUptime > 0) {
                pm.setCpu(parse(100d * (p.getKernelTime() + p.getUserTime()) / procUptime));
            } else {
                pm.setCpu(0.0);
            }

            pm.setMem(parse(100d * p.getResidentSetSize() / memory.getTotal()));
            processList.add(pm);
        }

        try {
            // 序列化为 JSON 字符串
            model.setTopProcessesJson(MAPPER.writeValueAsString(processList));
        } catch (Exception e) {
            model.setTopProcessesJson("[]");
        }

        return model;
    }

    private static double parse(double val) {
        if (Double.isNaN(val)) return 0.00;
        return Double.parseDouble(TWO_DECIMAL.format(val));
    }

    private static String getLocalIp() {
        String candidateIp = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                String name = ni.getName();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()
                        || name.contains("docker") || name.contains("br-") || name.contains("veth")) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        String ipStr = ip.getHostAddress();
                        if (ipStr.startsWith("192.168")) return ipStr;
                        if (ipStr.startsWith("10.")) return ipStr;
                        if (candidateIp == null) candidateIp = ipStr;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return candidateIp != null ? candidateIp : "127.0.0.1";
    }

    private static double formatLoad(double val) {
        return val < 0 ? 0.0 : Double.parseDouble(TWO_DECIMAL.format(val));
    }
}