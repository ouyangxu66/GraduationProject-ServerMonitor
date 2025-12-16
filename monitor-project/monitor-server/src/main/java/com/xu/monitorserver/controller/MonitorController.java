package com.xu.monitorserver.controller;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.monitorservice.IMonitorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final IMonitorService monitorService;

    public MonitorController(IMonitorService monitorService){
        this.monitorService = monitorService;
    }

    /**
     * 上报数据
     */
    @PostMapping("/report")
    public R<Void> report(@RequestBody BaseMonitorModel data) {
        monitorService.saveMonitorData(data);
        return R.ok();
    }

    /**
     * 获取CPU负载历史数据
     */
    @GetMapping("/cpu-history")
    public R<List<Map<String,Object>>> getCpuHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getCpuHistory(ip, start, end));
    }

    /**
     * 获取磁盘使用率历史数据
     */
    @GetMapping("/disk-history")
    public R<List<Map<String,Object>>> getDiskHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getDiskHistory(ip, start, end));
    }

    /**
     * 获取网络流量历史数据
     */
    @GetMapping("/net-history")
    public R<List<Map<String,Object>>> getNetHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getNetHistory(ip, start, end));
    }

    // 🟢 修复：基础信息不需要时间范围
    @GetMapping("/base-info")
    public R<Map<String,Object>> getBaseInfo(@RequestParam("ip") String ip){
        return R.ok(monitorService.getServerLatestInfo(ip));
    }

    /**
     * 获取系统负载历史数据
     */
    @GetMapping("/load-history")
    public R<Map<String,Object>> getSystemLoadHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getSystemLoadHistory(ip, start, end));
    }

    /**
     * 获取磁盘IO历史数据
     */
    @GetMapping("/disk-io-history")
    public R<Map<String,Object>> getDiskIoHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getDiskIoHistory(ip, start, end));
    }

    /**
     * 获取CPU温度历史数据
     */
    @GetMapping("/temp-history")
    public R<List<Map<String,Object>>> getTempHistory(
            @RequestParam("ip") String ip,
            @RequestParam(value = "start", required = false) String start,
            @RequestParam(value = "end", required = false) String end){
        return R.ok(monitorService.getTempHistory(ip, start, end));
    }
}