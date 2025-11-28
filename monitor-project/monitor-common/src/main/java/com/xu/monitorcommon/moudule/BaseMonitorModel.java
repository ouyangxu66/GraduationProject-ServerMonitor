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

// 我们暂时先不搞磁盘和网络，先把这几个最核心的跑通
}