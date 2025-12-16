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
 * InfluxDB 数据访问层 (Repository)
 * 负责所有与时序数据库的交互：写入 Point、执行 Flux 查询
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
     * 写入单条监控数据 (Write)
     * Client 端上报后调用此方法
     * @param model 监控数据传输对象
     */
    public void save(BaseMonitorModel model) {
        // 获取同步写入 API
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        // 构建数据点 (Point)
        // Measurement: 表名 "server_status"
        Point point = Point.measurement("server_status")
                // --- Tags (索引字段) ---
                // Tags 用于快速筛选，例如查询特定 IP 或 OS 的数据
                .addTag("os_name", model.getOsName())
                .addTag("host_name", model.getHostName())
                .addTag("ip", model.getIp())

                // --- Fields (数值字段) ---
                // 核心指标
                .addField("cpu_load", model.getCpuLoad())
                .addField("memory_used", model.getMemoryUsed())
                .addField("memory_total", model.getMemoryTotal())
                // 磁盘与网络
                .addField("disk_usage", model.getDiskUsage())
                .addField("disk_total", model.getDiskTotal())
                .addField("net_recv_rate", model.getNetRecvRate())
                .addField("net_sent_rate", model.getNetSentRate())
                // 负载与运行时间
                .addField("sys_load_1", model.getSystemLoad1())
                .addField("sys_load_5", model.getSystemLoad5())
                .addField("sys_load_15", model.getSystemLoad15())
                .addField("up_time", model.getUpTime())
                // 磁盘I/O速率与Top5进程列表
                .addField("disk_read_rate", model.getDiskReadRate())
                .addField("disk_write_rate", model.getDiskWriteRate())
                .addField("top_processes", model.getTopProcessesJson() !=null
                ?model.getTopProcessesJson():"[]")
                // CPU温度
                .addField("cpu_temp",model.getCpuTemperature())

                // 时间戳：使用当前服务器时间
                .time(Instant.now(), WritePrecision.NS);

        // 执行写入
        writeApi.writePoint(bucket, org, point);
    }

    /**
     * 通用查询历史数据 (支持动态时间)
     * @param start 开始时间 (ISO格式, 如 "-1h" 或 "2025-12-16T10:00:00Z")
     * @param end   结束时间 (ISO格式, 如 "now()" 或 "2025-12-16T12:00:00Z")
     */
    public List<Map<String, Object>> queryHistory(String ip, String field, String start, String end) {
        // 如果没传时间，默认查过去 1 小时
        String rangeStart = (start == null || start.isEmpty()) ? "-1h" : start;
        String rangeStop = (end == null || end.isEmpty()) ? "now()" : end;

        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s, stop: %s) " + // 🟢 动态注入 start 和 stop
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"server_status\") " +
                        "|> filter(fn: (r) => r[\"ip\"] == \"%s\") " +
                        "|> filter(fn: (r) => r[\"_field\"] == \"%s\") " +
                        "|> aggregateWindow(every: 10s, fn: mean, createEmpty: false) " +
                        "|> yield(name: \"mean\")",
                bucket, rangeStart, rangeStop, ip, field
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
     * 查询服务器最新基础信息 (Query Latest Info)
     * 用于前端顶部的基础信息卡片
     *
     * @param ip 服务器IP
     * @return Map { osName: "...", uptime: 12345, ... }
     */
    public Map<String, Object> queryLastOne(String ip) {
        // Flux 语句：只取最后一条 (last)
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

        // 解析逻辑：将分散在多行的 Field 聚合到一个 Map 中
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                // key 是字段名 (如 cpu_load, up_time)
                String key = record.getField();
                // val 是对应的值
                Object val = record.getValue();

                // 1. 提取 Tags (只需提取一次)
                if (info.isEmpty()) {
                    info.put("osName", record.getValueByKey("os_name"));
                    info.put("hostName", record.getValueByKey("host_name"));
                    info.put("ip", record.getValueByKey("ip"));
                }

                // 2. 提取 Fields 并转换为驼峰命名 (匹配前端)
                if ("memory_total".equals(key)) info.put("memoryTotal", val);
                if ("disk_total".equals(key)) info.put("diskTotal", val);
                // 数据库是 "up_time" -> 前端要 "uptime"
                if ("up_time".equals(key)) info.put("uptime", val);
                if ("top_processes".equals(key)) info.put("topProcesses", val);
            }
        }
        return info;
    }
}