package com.xu.monitorserver.service.monitorservice;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import java.util.List;
import java.util.Map;

public interface IMonitorService {
    // 保存数据
    void saveMonitorData(BaseMonitorModel model);

    // 获取各类历史数据
    //1. 获取CPU使用率历史数据
    List<Map<String, Object>> getCpuHistory(String ip);
    //2. 获取内存使用率历史数据
    List<Map<String, Object>> getDiskHistory(String ip);
    //3. 获取网络流量历史数据
    List<Map<String, Object>> getNetHistory(String ip);

    // 获取服务器基础信息
    Map<String, Object> getServerLatestInfo(String ip);

    Map<String, Object> getSystemLoadHistory(String ip);
}