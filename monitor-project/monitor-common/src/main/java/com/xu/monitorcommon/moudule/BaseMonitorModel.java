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

    //  系统负载(1分钟,5分钟,15分钟)
    private double systemLoad1;
    private double systemLoad5;
    private double systemLoad15;

    // 系统运行时间
    private long upTime;

    // 磁盘I/O速率(KB/s)
    private double diskReadRate;
    private double diskWriteRate;

    //  Top 5 进程 (JSON 字符串形式存储)
    private String topProcessesJson;
    // 辅助字段：仅用于传输，不存数据库,为了方便，我们在 Util 里直接转 JSON存入上面的String

    //CPU温度
    private double cpuTemperature;

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

    public double getSystemLoad1() {
        return systemLoad1;
    }

    public void setSystemLoad1(double systemLoad1) {
        this.systemLoad1 = systemLoad1;
    }

    public double getSystemLoad5() {
        return systemLoad5;
    }

    public void setSystemLoad5(double systemLoad5) {
        this.systemLoad5 = systemLoad5;
    }

    public double getSystemLoad15() {
        return systemLoad15;
    }

    public void setSystemLoad15(double systemLoad15) {
        this.systemLoad15 = systemLoad15;
    }

    public long getUpTime() {
        return upTime;
    }

    public void setUpTime(long upTime) {
        this.upTime = upTime;
    }

    public double getDiskReadRate() {
        return diskReadRate;
    }

    public void setDiskReadRate(double diskReadRate) {
        this.diskReadRate = diskReadRate;
    }

    public double getDiskWriteRate() {
        return diskWriteRate;
    }

    public void setDiskWriteRate(double diskWriteRate) {
        this.diskWriteRate = diskWriteRate;
    }

    public String getTopProcessesJson() {
        return topProcessesJson;
    }

    public void setTopProcessesJson(String topProcessesJson) {
        this.topProcessesJson = topProcessesJson;
    }

    public double getCpuTemperature() {
        return cpuTemperature;
    }

    public void setCpuTemperature(double cpuTemperature) {
        this.cpuTemperature = cpuTemperature;
    }
}