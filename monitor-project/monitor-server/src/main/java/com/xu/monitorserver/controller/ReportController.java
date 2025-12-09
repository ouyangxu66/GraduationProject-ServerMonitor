package com.xu.monitorserver.controller;

import com.xu.monitorcommon.moudule.BaseMonitorModel;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.monitorservice.MonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/monitor")
public class ReportController {
    //手动声明Logger常量,用来获取日志信息
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private MonitorService monitorService;
    @PostMapping("/report")
    public R<BaseMonitorModel> reportData(@RequestBody BaseMonitorModel model){
        //将监控数据存入到InfluxDB
        monitorService.saveMonitorData(model);
        return R.ok(model);
    }
}
