package com.xu.monitorcommon.utils;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控工具类
 * 用于收集系统运行时的各种指标，包括CPU、内存、磁盘和网络使用情况
 */
public class SystemMonitorUtil {

    private static final SystemInfo SI = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    private static final OperatingSystem OS = SI.getOperatingSystem();
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("#.00");

    /**
     * 收集系统监控数据
     * 包括操作系统信息、主机信息、内存使用情况、磁盘使用情况以及CPU和网络流量等指标
     * 
     * @return BaseMonitorModel 系统监控数据模型
     * @throws InterruptedException 当线程在采样间隔中被中断时抛出
     */
    public static BaseMonitorModel collect() throws InterruptedException {
        BaseMonitorModel model = new BaseMonitorModel();

        // 1. 基础信息
        model.setOsName(OS.toString());
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            model.setHostName(localHost.getHostName());
            model.setIp(localHost.getHostAddress());
        } catch (UnknownHostException e) {
            model.setHostName("Unknown");
            model.setIp("127.0.0.1");
        }

        // 2. 内存信息
        GlobalMemory memory = HAL.getMemory();
        double totalMem = memory.getTotal() / 1024.0 / 1024.0 / 1024.0;
        double usedMem = (memory.getTotal() - memory.getAvailable()) / 1024.0 / 1024.0 / 1024.0;
        model.setMemoryTotal(parse(totalMem));
        model.setMemoryUsed(parse(usedMem));

        // 3. 磁盘信息 (累加所有分区)
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

        // 4. CPU & 网络流量 (需要采样)
        CentralProcessor processor = HAL.getProcessor();
        List<NetworkIF> networkIFs = HAL.getNetworkIFs();

        // 4.1 第一次采样
        long[] prevCpuTicks = processor.getSystemCpuLoadTicks();
        long prevRecv = 0;
        long prevSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // 更新网卡状态
            prevRecv += net.getBytesRecv();
            prevSent += net.getBytesSent();
        }

        // --- 休眠 1 秒 ---
        TimeUnit.SECONDS.sleep(1);

        // 4.2 第二次采样
        long[] currCpuTicks = processor.getSystemCpuLoadTicks();
        long currRecv = 0;
        long currSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            currRecv += net.getBytesRecv();
            currSent += net.getBytesSent();
        }

        // 4.3 计算差值
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        // 速率 = (第二次总量 - 第一次总量) / 1秒 -> 结果单位 Byte/s -> 转为 KB/s
        double netRecvRate = (currRecv - prevRecv) / 1024.0;
        double netSentRate = (currSent - prevSent) / 1024.0;

        model.setCpuLoad(parse(cpuLoad));
        model.setNetRecvRate(parse(netRecvRate));
        model.setNetSentRate(parse(netSentRate));

        return model;
    }

    /**
     * 将数值格式化为保留两位小数的double值
     * 
     * @param val 需要格式化的原始数值
     * @return double 格式化后的数值
     */
    private static double parse(double val) {
        return Double.parseDouble(TWO_DECIMAL.format(val));
    }
}