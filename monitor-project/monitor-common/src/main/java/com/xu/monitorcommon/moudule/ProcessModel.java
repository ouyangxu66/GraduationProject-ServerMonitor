package com.xu.monitorcommon.moudule;

public class ProcessModel {
    private String name;
    private int pid;        // 进程ID
    private double cpu;     // CPU占用率 %
    private double mem;     // 内存占用率 %// 进程名

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getCpu() {
        return cpu;
    }

    public void setCpu(double cpu) {
        this.cpu = cpu;
    }

    public double getMem() {
        return mem;
    }

    public void setMem(double mem) {
        this.mem = mem;
    }

}
