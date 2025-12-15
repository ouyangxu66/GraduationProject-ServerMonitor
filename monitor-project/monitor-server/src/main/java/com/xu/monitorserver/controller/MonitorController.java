package com.xu.monitorserver.controller;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.monitorservice.IMonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
// 🟢 确保路径前缀是 /api/monitor
@RequestMapping("/api/monitor")
public class MonitorController {

    private final IMonitorService monitorService;

    // 推荐使用构造器注入，并设为 final
    public MonitorController(IMonitorService monitorService){
        this.monitorService = monitorService;
    }

    /**
     * 接收探针上报的数据
     * Client端配置: monitor.server-url=http://ip:port/api/monitor/report
     */
    @PostMapping("/report")
    public R<Void> report(@RequestBody BaseMonitorModel data) {
        monitorService.saveMonitorData(data);
        return R.ok();
    }

    /**
     * 获取 CPU 历史数据
     * 前端: getCpuHistory({ ip: '...' })
     */
    @GetMapping("/cpu-history")
    public R<List<Map<String,Object>>> getCpuHistory(@RequestParam("ip") String ip){
        // 🟢 传入 IP 参数
        return R.ok(monitorService.getCpuHistory(ip));
    }

    /**
     * 获取 磁盘 历史数据
     * 前端: getDiskHistory({ ip: '...' })
     */
    @GetMapping("/disk-history")
    public R<List<Map<String,Object>>> getDiskHistory(@RequestParam("ip") String ip){
        return R.ok(monitorService.getDiskHistory(ip));
    }

    /**
     * 获取 网络 历史数据
     * 前端: getNetHistory({ ip: '...' })
     */
    @GetMapping("/net-history")
    public R<List<Map<String,Object>>> getNetHistory(@RequestParam("ip") String ip){
        return R.ok(monitorService.getNetHistory(ip));
    }

    /**
     * 获取服务器基础信息 (OS, HostName, TotalMem 等)
     * 前端: getServerBaseInfo({ ip: '...' })
     */
    @GetMapping("/base-info")
    public R<Map<String,Object>> getBaseInfo(@RequestParam("ip") String ip){
        return R.ok(monitorService.getServerLatestInfo(ip));
    }

    /**
     * 获取服务器系统负载历史数据
     * 前端: getSystemHistory({ ip: '...' })
     */
    @GetMapping("/load-history")
    public R<Map<String,Object>> getSystemLoadHistory(@RequestParam("ip") String ip){
        return R.ok(monitorService.getSystemLoadHistory(ip));
    }
}