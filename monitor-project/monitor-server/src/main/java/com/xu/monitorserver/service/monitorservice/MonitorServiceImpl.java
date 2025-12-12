package com.xu.monitorserver.service.monitorservice;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorserver.repository.InfluxRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MonitorServiceImpl implements IMonitorService {

    private final InfluxRepository influxRepository;

    // 构造器注入 Repository
    public MonitorServiceImpl(InfluxRepository influxRepository) {
        this.influxRepository = influxRepository;
    }

    @Override
    public void saveMonitorData(BaseMonitorModel model) {
        // 可以在这里做一些业务校验，比如判断 ip 是否合法等
        influxRepository.save(model);
    }

    @Override
    public List<Map<String, Object>> getCpuHistory(String ip) {
        // 调用 Repository 查询 cpu_load 字段
        return influxRepository.queryHistory(ip, "cpu_load");
    }

    @Override
    public List<Map<String, Object>> getDiskHistory(String ip) {
        // 查询 disk_usage 字段
        return influxRepository.queryHistory(ip, "disk_usage");
    }

    @Override
    public List<Map<String, Object>> getNetHistory(String ip) {
        // 查询 net_recv_rate 字段 (下行速率)
        return influxRepository.queryHistory(ip, "net_recv_rate");
    }

    @Override
    public Map<String, Object> getServerLatestInfo(String ip) {
        return influxRepository.queryLastOne(ip);
    }
}