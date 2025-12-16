package com.xu.monitorserver.service.monitorservice;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import java.util.List;
import java.util.Map;

public interface IMonitorService {
    // 保存数据
    void saveMonitorData(BaseMonitorModel model);

    // 获取各类历史数据
    List<Map<String, Object>> getCpuHistory(String ip, String start, String end);
    // 获取磁盘使用率
    List<Map<String, Object>> getDiskHistory(String ip, String start, String end);
    // 获取网络流量
    List<Map<String, Object>> getNetHistory(String ip, String start, String end);
    // 获取服务器最新信息
    Map<String, Object> getServerLatestInfo(String ip);
    // 获取系统负载
    Map<String, Object> getSystemLoadHistory(String ip, String start, String end);
    // 获取磁盘IO
    Map<String, Object> getDiskIoHistory(String ip, String start, String end);
    // 获取CPU温度
    List<Map<String, Object>> getTempHistory(String ip, String start, String end);
}