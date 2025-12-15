package com.xu.monitorcommon.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * 系统监控工具类
 * 用于收集系统的各种性能指标，包括CPU、内存、磁盘、网络等信息
 */
public class SystemMonitorUtil {

    /**
     * 系统信息实例 - OSHI库的核心入口点
     * OSHI是一个用于获取操作系统和硬件信息的Java库
     */
    private static final SystemInfo SI = new SystemInfo();
    
    /**
     * 硬件抽象层实例
     * 提供对硬件组件（如CPU、内存、网络接口等）的访问
     */
    private static final HardwareAbstractionLayer HAL = SI.getHardware();
    
    /**
     * 操作系统实例
     * 提供对操作系统相关信息的访问
     */
    private static final OperatingSystem OS = SI.getOperatingSystem();
    
    /**
     * 数字格式化器 - 保留两位小数
     */
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("#.00");
    
    /**
     * JSON对象映射器 - 用于序列化和反序列化JSON数据
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 收集系统监控数据的主要方法
     * 该方法会收集包括基础信息、内存、磁盘、CPU和网络等各项指标
     *
     * @return BaseMonitorModel 包含所有监控数据的模型对象
     * @throws InterruptedException 当线程在sleep期间被中断时抛出
     */
    public static BaseMonitorModel collect() throws InterruptedException {
        // 创建监控数据模型对象
        BaseMonitorModel model = new BaseMonitorModel();

        // 1. 收集基础系统信息
        // 获取操作系统名称和版本信息
        model.setOsName(OS.toString());
        // 获取主机名
        model.setHostName(OS.getNetworkParams().getHostName());
        // 获取本机IP地址
        model.setIp(getLocalIp());
        // 获取系统运行时间（秒）
        model.setUpTime(OS.getSystemUptime());

        // 2. 收集内存信息
        // 获取全局内存信息
        GlobalMemory memory = HAL.getMemory();
        // 计算总内存大小（转换为GB单位）
        double totalMem = memory.getTotal() / 1024.0 / 1024.0 / 1024.0;
        // 计算已使用内存大小（转换为GB单位）
        double usedMem = (memory.getTotal() - memory.getAvailable()) / 1024.0 / 1024.0 / 1024.0;
        // 设置总内存和已使用内存（保留两位小数）
        model.setMemoryTotal(parse(totalMem));
        model.setMemoryUsed(parse(usedMem));

        // 3. 收集磁盘信息
        // 获取文件系统信息
        FileSystem fileSystem = OS.getFileSystem();
        // 获取所有文件存储信息
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        // 初始化磁盘总空间和已使用空间计数器
        long totalDiskBytes = 0;
        long usedDiskBytes = 0;
        // 遍历所有文件存储设备，累加总空间和已使用空间
        for (OSFileStore fs : fileStores) {
            totalDiskBytes += fs.getTotalSpace();
            usedDiskBytes += (fs.getTotalSpace() - fs.getUsableSpace());
        }
        // 将字节转换为GB单位
        double totalDiskGb = totalDiskBytes / 1024.0 / 1024.0 / 1024.0;
        double usedDiskGb = usedDiskBytes / 1024.0 / 1024.0 / 1024.0;
        // 设置磁盘总空间、已使用空间和使用率
        model.setDiskTotal(parse(totalDiskGb));
        model.setDiskUsed(parse(usedDiskGb));
        // 计算磁盘使用率百分比
        model.setDiskUsage(totalDiskGb > 0 ? parse((usedDiskGb / totalDiskGb) * 100) : 0);

        // 4. 收集CPU和网络流量信息
        // 获取中央处理器信息
        CentralProcessor processor = HAL.getProcessor();
        // 获取网络接口列表
        List<NetworkIF> networkIFs = HAL.getNetworkIFs();

        // 获取初始CPU ticks（用于计算CPU使用率）
        long[] prevCpuTicks = processor.getSystemCpuLoadTicks();
        // 初始化网络接收和发送字节数计数器
        long prevRecv = 0;
        long prevSent = 0;
        // 遍历所有网络接口，累加初始接收和发送字节数
        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // 更新网络接口属性
            prevRecv += net.getBytesRecv(); // 累加接收字节数
            prevSent += net.getBytesSent(); // 累加发送字节数
        }

        // 等待1秒钟，以便计算速率
        TimeUnit.SECONDS.sleep(1);

        // 获取当前CPU ticks
        long[] currCpuTicks = processor.getSystemCpuLoadTicks();
        // 重新初始化网络接收和发送字节数计数器
        long currRecv = 0;
        long currSent = 0;
        // 再次遍历所有网络接口，累加当前接收和发送字节数
        for (NetworkIF net : networkIFs) {
            net.updateAttributes(); // 更新网络接口属性
            currRecv += net.getBytesRecv(); // 累加接收字节数
            currSent += net.getBytesSent(); // 累加发送字节数
        }

        // 计算CPU使用率（百分比）
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevCpuTicks) * 100;
        // 计算网络接收速率（KB/s）
        double netRecvRate = (currRecv - prevRecv) / 1024.0;
        // 计算网络发送速率（KB/s）
        double netSentRate = (currSent - prevSent) / 1024.0;
        // 设置CPU和网络相关指标（保留两位小数）
        model.setCpuLoad(parse(cpuLoad));
        model.setNetRecvRate(parse(netRecvRate));
        model.setNetSentRate(parse(netSentRate));

        // 5. 获取系统负载信息
        // 获取系统负载平均值数组，分别对应1分钟、5分钟、15分钟的负载
        double[] loads = HAL.getProcessor().getSystemLoadAverage(3);
        // 设置系统负载指标（处理可能的无效值）
        model.setSystemLoad1(formatLoad(loads[0]));
        model.setSystemLoad5(formatLoad(loads[1]));
        model.setSystemLoad15(formatLoad(loads[2]));

        // 返回填充完所有数据的监控模型对象
        return model;
    }

    /**
     * 辅助方法：格式化数值，保留两位小数
     * @param val 需要格式化的原始数值
     * @return 保留两位小数的double值
     */
    private static double parse(double val) {
        return Double.parseDouble(TWO_DECIMAL.format(val));
    }

    /**
     * 辅助方法：智能获取本机真实IP地址
     * 优先级顺序：192.168.x.x > 10.x.x.x > 172.x.x.x（非Docker环境）
     * 排除回环地址、虚拟接口、Docker相关接口
     *
     * @return 本机IP地址字符串
     */
    private static String getLocalIp() {
        // 候选IP地址，当没有找到高优先级IP时使用
        String candidateIp = null;
        try {
            // 获取所有网络接口枚举
            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            // 遍历所有网络接口
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                String name = ni.getName();

                // 1. 排除不符合条件的网络接口
                // 排除未启用、回环、虚拟接口以及Docker相关的网络接口
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()
                        || name.contains("docker") || name.contains("br-") || name.contains("veth")) {
                    continue;
                }

                // 获取该网络接口的所有IP地址
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                // 遍历所有IP地址
                while (addresses.hasMoreElements()) {
                    InetAddress ip = addresses.nextElement();
                    // 只处理IPv4地址
                    if (ip instanceof Inet4Address) {
                        String ipStr = ip.getHostAddress();

                        // 2. 优先返回192.168开头的IP地址（最常见的局域网地址）
                        if (ipStr.startsWith("192.168")) {
                            return ipStr;
                        }
                        // 3. 其次返回10.开头的IP地址
                        if (ipStr.startsWith("10.")) {
                            return ipStr;
                        }
                        // 4. 暂存其他IP地址（如172.x.x.x），作为备选方案
                        if (candidateIp == null) {
                            candidateIp = ipStr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 异常处理：打印堆栈跟踪信息
            e.printStackTrace();
        }
        // 如果没有找到192.168或10.开头的IP，则返回暂存的IP，最后兜底返回127.0.0.1
        return candidateIp != null ? candidateIp : "127.0.0.1";
    }

    /**
     * 辅助方法：格式化系统负载数值
     * 处理Windows系统可能返回负数的情况（表示不支持）
     *
     * @param val 原始负载值
     * @return 格式化后的负载值（非负数，保留两位小数）
     */
    private static double formatLoad(double val) {
        // 如果值为负数，则返回0.0；否则保留两位小数
        return val < 0 ? 0.0 : Double.parseDouble(TWO_DECIMAL.format(val));
    }
}