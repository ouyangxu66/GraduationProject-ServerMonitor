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

                // 时间戳：使用当前服务器时间
                .time(Instant.now(), WritePrecision.NS);

        // 执行写入
        writeApi.writePoint(bucket, org, point);
    }

    /**
     * 查询历史趋势数据 (Query History)
     * 用于前端绘制折线图
     * @param ip 服务器IP
     * @param field 数据库中的字段名 (如 cpu_load, disk_usage)
     * @return List [{time: "...", value: 12.5}, ...]
     */
    public List<Map<String, Object>> queryHistory(String ip, String field) {
        // 构造 Flux 查询语句
        // 1. 指定 Bucket
        // 2. range: 查询过去 1 小时
        // 3. filter: 筛选表、IP、字段
        // 4. aggregateWindow: 降采样，每 10 秒取一个平均值，防止数据点过多
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

        // 执行查询
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);
        List<Map<String, Object>> result = new ArrayList<>();

        // 解析结果：InfluxDB 返回的是 Table -> Record 结构
        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> map = new HashMap<>();
                map.put("time", record.getTime().toString()); // ISO 时间格式
                map.put("value", record.getValue());          // 数值
                result.add(map);
            }
        }
        return result;
    }

    /**
     * 查询服务器最新基础信息 (Query Latest Info)
     * 用于前端顶部的基础信息卡片
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