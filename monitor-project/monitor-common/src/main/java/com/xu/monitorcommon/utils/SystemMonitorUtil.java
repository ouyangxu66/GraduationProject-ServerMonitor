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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SystemMonitorUtil {

    private static final SystemInfo SI = new SystemInfo();
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    private static final OperatingSystem OS = SI.getOperatingSystem();
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("#.00");

    public static BaseMonitorModel collect() throws InterruptedException {
        BaseMonitorModel model = new BaseMonitorModel();

        // 1. 基础信息
        model.setOsName(OS.toString());
        model.setHostName(OS.getNetworkParams().getHostName());
        // 🟢 核心修改：使用优化后的 IP 获取逻辑
        model.setIp(getLocalIp());

        // 2. 内存信息
        GlobalMemory memory = HAL.getMemory();
        double totalMem = memory.getTotal() / 1024.0 / 1024.0 / 1024.0;
        double usedMem = (memory.getTotal() - memory.getAvailable()) / 1024.0 / 1024.0 / 1024.0;
        model.setMemoryTotal(parse(totalMem));
        model.setMemoryUsed(parse(usedMem));

        // 3. 磁盘信息
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

        // 4. CPU & 网络流量
        CentralProcessor processor = HAL.getProcessor();
        List<NetworkIF> networkIFs = HAL.getNetworkIFs();

        long[] prevCpuTicks = processor.getSystemCpuLoadTicks();
        long prevRecv = 0;
        long prevSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            prevRecv += net.getBytesRecv();
            prevSent += net.getBytesSent();
        }

        TimeUnit.SECONDS.sleep(1);

        long[] currCpuTicks = processor.getSystemCpuLoadTicks();
        long currRecv = 0;
        long currSent = 0;
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            currRecv += net.getBytesRecv();
            currSent += net.getBytesSent();
        }

        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        double netRecvRate = (currRecv - prevRecv) / 1024.0;
        double netSentRate = (currSent - prevSent) / 1024.0;

        //5.获取系统负载(返回一个数组,分别对应1min,5min,15min)
        double[] loads = HAL.getProcessor().getSystemLoadAverage(3);

        // 设置模型参数
        model.setSystemLoad1(formatLoad(loads[0]));
        model.setSystemLoad5(formatLoad(loads[1]));
        model.setSystemLoad15(formatLoad(loads[2]));
        model.setCpuLoad(parse(cpuLoad));
        model.setNetRecvRate(parse(netRecvRate));
        model.setNetSentRate(parse(netSentRate));
        model.setUpTime(OS.getSystemUptime());

        return model;
    }

    /**
     *辅助方法:获取小数点后两位
     */
    private static double parse(double val) {
        return Double.parseDouble(TWO_DECIMAL.format(val));
    }

    /**
     * 辅助方法:智能获取真实 IP
     * 优先级：192.168 > 10. > 172. (非 Docker)
     */
    private static String getLocalIp() {
        String candidateIp = null;
        try {
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                String name = ni.getName();

                // 1. 排除回环、虚拟、未启动、Docker 网桥
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()
                        || name.contains("docker") || name.contains("br-") || name.contains("veth")) {
                    continue;
                }

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        String ipStr = ip.getHostAddress();

                        // 2. 优先返回 192.168 开头的 (最常见的局域网 IP)
                        if (ipStr.startsWith("192.168")) {
                            return ipStr;
                        }
                        // 3. 其次返回 10. 开头的
                        if (ipStr.startsWith("10.")) {
                            return ipStr;
                        }
                        // 4. 暂存其他 IP (如 172.x，但要在最后才用)
                        if (candidateIp == null) {
                            candidateIp = ipStr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 如果没找到 192.168 或 10. 的，就返回暂存的，最后兜底 127.0.0.1
        return candidateIp != null ? candidateIp : "127.0.0.1";
    }

    /**
     * 辅助方法：处理负载数值，防止 Windows 返回负数
     */
    private static double formatLoad(double val) {
        return val < 0 ? 0.0 : Double.parseDouble(TWO_DECIMAL.format(val));
    }
}