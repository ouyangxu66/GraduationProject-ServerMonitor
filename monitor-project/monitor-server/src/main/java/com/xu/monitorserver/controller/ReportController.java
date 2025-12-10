package com.xu.monitorserver.controller;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.monitorservice.IMonitorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/monitor")
public class ReportController {

    private IMonitorService monitorService;

    public ReportController(IMonitorService monitorService){
        this.monitorService = monitorService;
    }
    @PostMapping("/report")
    public R<BaseMonitorModel> reportData(@RequestBody BaseMonitorModel model){
        //将监控数据存入到InfluxDB
        monitorService.saveMonitorData(model);
        return R.ok(model);
    }
}
