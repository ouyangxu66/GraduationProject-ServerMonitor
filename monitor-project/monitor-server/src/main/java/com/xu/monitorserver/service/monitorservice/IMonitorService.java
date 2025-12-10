package com.xu.monitorserver.service.monitorservice;

import com.xu.monitorcommon.moudule.BaseMonitorModel;

import java.util.List;
import java.util.Map;

public interface IMonitorService {

    /**
     * 保存监控数据
     * @param model 传输对象
     */
    void saveMonitorData(BaseMonitorModel model);

    /**
     * 查询 CPU 历史趋势
     * @return 列表数据
     */
    List<Map<String, Object>> queryCpuHistory();
}