package com.xu.monitorserver.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.xu.monitorcommon.moudule.BaseMonitorModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InfluxDB 操作类
 */
@Repository
public class InfluxRepository {

    private final InfluxDBClient influxDBClient;

    public InfluxRepository(InfluxDBClient influxDBClient) {
        this.influxDBClient = influxDBClient;
    }

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String org;

    /**
     * 写入单条监控数据
     */
    public void save(BaseMonitorModel model) {
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        // 构建数据点
        Point point = Point.measurement("server_status")
                // Tags: 用于索引和筛选
                .addTag("os_name", model.getOsName())
                .addTag("host_name", model.getHostName())
                .addTag("ip", model.getIp())
                // Fields: 实际的数值数据 (这里补全了 CPU、内存、磁盘、网络)
                .addField("cpu_load", model.getCpuLoad())
                .addField("memory_used", model.getMemoryUsed())
                .addField("memory_total", model.getMemoryTotal())
                .addField("disk_usage", model.getDiskUsage())
                .addField("disk_total", model.getDiskTotal())
                .addField("net_recv_rate", model.getNetRecvRate())
                .addField("net_sent_rate", model.getNetSentRate())
                .time(Instant.now(), WritePrecision.NS);

        writeApi.writePoint(bucket, org, point);
    }

    /**
     * 通用查询历史数据方法
     * @param ip 服务器IP
     * @param field 数据库中的字段名 (如 cpu_load, disk_usage)
     */
    public List<Map<String, Object>> queryHistory(String ip, String field) {
        // 动态构建 Flux 语句
        // 增加 filter(ip) 确保只查询当前选中的服务器
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r[\"_measurement\"] == \"server_status\") " +
            "|> filter(fn: (r) => r[\"ip\"] == \"%s\") " +
            "|> filter(fn: (r) => r[\"_field\"] == \"%s\") " +
            "|> aggregateWindow(every: 10s, fn: mean, createEmpty: false) " +
            "|> yield(name: \"mean\")",
            bucket, ip, field
        );

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);
        List<Map<String, Object>> result = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> map = new HashMap<>();
                map.put("time", record.getTime().toString());
                map.put("value", record.getValue());
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 查询最新的一条数据 (用于展示基础信息)
     */
    public Map<String, Object> queryLastOne(String ip) {
        String flux = String.format(
            "from(bucket: \"%s\") " +
            "|> range(start: -1h) " +
            "|> filter(fn: (r) => r[\"_measurement\"] == \"server_status\") " +
            "|> filter(fn: (r) => r[\"ip\"] == \"%s\") " +
            "|> last()",
            bucket, ip
        );

        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);
        Map<String, Object> info = new HashMap<>();

        // 将分散的 Field 聚合到一个 Map 中
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                String key = record.getField();
                Object val = record.getValue();
                // 同时也获取 Tag 信息 (如 os_name)
                if (info.isEmpty()) {
                    info.put("osName", record.getValueByKey("os_name"));
                    info.put("hostName", record.getValueByKey("host_name"));
                    info.put("ip", record.getValueByKey("ip"));
                }
                // 转换字段名以匹配前端驼峰命名
                if ("memory_total".equals(key)) info.put("memoryTotal", val);
                if ("disk_total".equals(key)) info.put("diskTotal", val);
            }
        }
        return info;
    }
}