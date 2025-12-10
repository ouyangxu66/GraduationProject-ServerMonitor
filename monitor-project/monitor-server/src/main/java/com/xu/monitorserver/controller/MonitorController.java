package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.service.monitorservice.IMonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/monitor")
public class MonitorController {

    private IMonitorService monitorService;

    public MonitorController(IMonitorService monitorService){
        this.monitorService = monitorService;
    }

    /**
     *
     */
    @GetMapping("/cpu-history")
    public R<List<Map<String,Object>>> getCpuHistory(){
        return R.ok(monitorService.queryCpuHistory());
    }

}
