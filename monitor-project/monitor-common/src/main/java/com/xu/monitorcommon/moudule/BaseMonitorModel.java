package com.xu.monitorcommon.moudule;



public class BaseMonitorModel {
    // 基础信息
    private String osName;      // 操作系统名称 (如 Windows 11)
    private String hostName;    // 主机名
    private String ip;          // IP地址

    // 核心硬件
    private double cpuLoad;     // CPU使用率 (0.0 - 100.0)
    private double memoryTotal; // 内存总量 (GB)
    private double memoryUsed;  // 内存已用 (GB)

    // 磁盘信息
    private double diskTotal;   // 磁盘总量 (GB)
    private double diskUsed;    // 磁盘已用 (GB)
    private double diskUsage;   // 磁盘使用率 (%)

    // 网络信息 (速率)
    private double netRecvRate; // 下行速率 (KB/s)
    private double netSentRate; // 上行速率 (KB/s)

    public String getOsName() {
        return osName;
    }

    public void setOsName(String osName) {
        this.osName = osName;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    public void setCpuLoad(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }

    public double getMemoryTotal() {
        return memoryTotal;
    }

    public void setMemoryTotal(double memoryTotal) {
        this.memoryTotal = memoryTotal;
    }

    public double getMemoryUsed() {
        return memoryUsed;
    }

    public void setMemoryUsed(double memoryUsed) {
        this.memoryUsed = memoryUsed;
    }

    public double getDiskTotal() {
        return diskTotal;
    }

    public void setDiskTotal(double diskTotal) {
        this.diskTotal = diskTotal;
    }

    public double getDiskUsed() {
        return diskUsed;
    }

    public void setDiskUsed(double diskUsed) {
        this.diskUsed = diskUsed;
    }

    public double getDiskUsage() {
        return diskUsage;
    }

    public void setDiskUsage(double diskUsage) {
        this.diskUsage = diskUsage;
    }

    public double getNetRecvRate() {
        return netRecvRate;
    }

    public void setNetRecvRate(double netRecvRate) {
        this.netRecvRate = netRecvRate;
    }

    public double getNetSentRate() {
        return netSentRate;
    }

    public void setNetSentRate(double netSentRate) {
        this.netSentRate = netSentRate;
    }
}