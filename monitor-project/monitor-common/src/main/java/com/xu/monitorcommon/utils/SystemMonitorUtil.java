package com.xu.monitorcommon.utils;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * 系统监控工具类
 * 用于收集系统硬件信息，包括CPU、内存和操作系统信息
 */
public class SystemMonitorUtil {

    //系统信息实例
    private static final SystemInfo SI = new SystemInfo();

    //硬件抽象层实例
    private static final HardwareAbstractionLayer HAL = SI.getHardware();

     //保留两位小数的格式化器
    private static final DecimalFormat TWO_DECIMAL = new DecimalFormat("#.00");

    /**
     * 收集系统监控数据
     * @return 包含系统信息的BaseMonitorModel对象
     */
    public static BaseMonitorModel collect() throws InterruptedException {
        BaseMonitorModel model = new BaseMonitorModel();

        // 1. 操作系统信息
        model.setOsName(SI.getOperatingSystem().toString());

        // 2. 内存信息
        GlobalMemory memory = HAL.getMemory();
        // 计算总内存(GB)
        double totalGb = memory.getTotal() / 1024.0 / 1024.0 / 1024.0;
        // 计算已使用内存(GB)
        double usedGb = (memory.getTotal() - memory.getAvailable()) / 1024.0 / 1024.0 / 1024.0;
        model.setMemoryTotal(Double.parseDouble(TWO_DECIMAL.format(totalGb)));
        model.setMemoryUsed(Double.parseDouble(TWO_DECIMAL.format(usedGb)));

        // 3. CPU信息 (注意：计算CPU负载通常需要一点时间间隔)
        CentralProcessor processor = HAL.getProcessor();
        // 获取CPU负载ticks
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        // 睡眠1秒，让CPU跑一会儿，才能计算出这段时间的负载
        TimeUnit.SECONDS.sleep(1);

        // 再次获取CPU负载ticks并计算CPU使用率
        long[] ticks = processor.getSystemCpuLoadTicks();
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
        model.setCpuLoad(Double.parseDouble(TWO_DECIMAL.format(cpuLoad)));

        return model;
    }

}
