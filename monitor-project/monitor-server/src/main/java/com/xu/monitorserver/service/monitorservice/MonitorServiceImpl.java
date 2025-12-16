package com.xu.monitorserver.service.monitorservice;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorserver.repository.InfluxRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorServiceImpl implements IMonitorService {

    private final InfluxRepository influxRepository;

    public MonitorServiceImpl(InfluxRepository influxRepository) {
        this.influxRepository = influxRepository;
    }


    @Override
    public void saveMonitorData(BaseMonitorModel model) {
        influxRepository.save(model);
    }

    @Override
    public List<Map<String, Object>> getCpuHistory(String ip, String start, String end) {
        return influxRepository.queryHistory(ip, "cpu_load", start, end);
    }

    @Override
    public List<Map<String, Object>> getDiskHistory(String ip, String start, String end) {
        return influxRepository.queryHistory(ip, "disk_usage", start, end);
    }

    @Override
    public List<Map<String, Object>> getNetHistory(String ip, String start, String end) {
        return influxRepository.queryHistory(ip, "net_recv_rate", start, end);
    }

    @Override
    public Map<String, Object> getServerLatestInfo(String ip) {
        return influxRepository.queryLastOne(ip);
    }

    @Override
    public Map<String, Object> getSystemLoadHistory(String ip, String start, String end){
        HashMap<String, Object> result = new HashMap<>();
        result.put("load1", influxRepository.queryHistory(ip, "sys_load_1", start, end));
        result.put("load5", influxRepository.queryHistory(ip, "sys_load_5", start, end));
        result.put("load15", influxRepository.queryHistory(ip, "sys_load_15", start, end));
        return result;
    }

    @Override
    public Map<String, Object> getDiskIoHistory(String ip, String start, String end) {
        Map<String, Object> result = new HashMap<>();
        result.put("read", influxRepository.queryHistory(ip, "disk_read_rate", start, end));
        result.put("write", influxRepository.queryHistory(ip, "disk_write_rate", start, end));
        return result;
    }

    @Override
    public List<Map<String, Object>> getTempHistory(String ip, String start, String end){
        return influxRepository.queryHistory(ip, "cpu_temp", start, end);
    }
}