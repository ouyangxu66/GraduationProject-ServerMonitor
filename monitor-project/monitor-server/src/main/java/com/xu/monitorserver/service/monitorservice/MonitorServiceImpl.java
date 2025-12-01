package com.xu.monitorserver.service.monitorservice;

import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.influxdb.client.InfluxDBClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MonitorServiceImpl implements MonitorService {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String org;


    @Override // <--- 加上 @Override 这是一个好习惯
    /**
     * 保存监控数据到 InfluxDB
     * @param model 监控数据模型
     */
    public void saveMonitorData(BaseMonitorModel model) {
        // 1. 获取同步写入接口
        WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

        // 2. 构建数据点 (Point)
        // Measurement: 相当于表名，这里叫 "server_status"
        Point point = Point.measurement("server_status")
                // Tag: 索引字段，查询时用来筛选的 (如：查询 host=MyComputer 的数据)
                .addTag("os", model.getOsName())
                .addTag("ip", model.getIp())
                // Field: 具体的数值，用于画图
                .addField("cpu_load", model.getCpuLoad())
                .addField("memory_used", model.getMemoryUsed())
                .addField("memory_total", model.getMemoryTotal())
                // Time: 写入当前时间
                .time(Instant.now(), WritePrecision.NS);

        // 3. 写入数据库
        writeApi.writePoint(bucket, org, point);
    }

    @Override
    /**
     * 查询过去一段时间的 CPU 使用率历史
     * @return 时间-数值 对的列表
     */
    public List<Map<String, Object>> queryCpuHistory() {
        // 1. 编写 Flux 查询脚本
        // 意思：从 monitor_bucket 桶里 -> 取过去 1 小时的数据 -> 筛选 server_status 表 -> 筛选 cpu_load 字段
        // -> 每 10 秒钟取一个平均值 (降采样，防止数据点太多前端渲染卡死)
        String flux = "from(bucket: \"" + bucket + "\")\n" +
                "  |> range(start: -1h)\n" +
                "  |> filter(fn: (r) => r[\"_measurement\"] == \"server_status\")\n" +
                "  |> filter(fn: (r) => r[\"_field\"] == \"cpu_load\")\n" +
                "  |> aggregateWindow(every: 10s, fn: mean, createEmpty: false)\n" +
                "  |> yield(name: \"mean\")";

        // 2. 执行查询
        List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, org);

        // 3. 将复杂的 Flux 结果转换为简单的 List<Map> 给前端
        List<Map<String, Object>> result = new ArrayList<>();

        for (FluxTable table : tables) {
            for (FluxRecord record : table.getRecords()) {
                Map<String, Object> map = new HashMap<>();
                // record.getTime() 拿到的是 Instant 时间对象
                map.put("time", record.getTime().toString());
                // record.getValue() 拿到的是 CPU 数值
                map.put("value", record.getValue());
                result.add(map);
            }
        }
        return result;
    }
}