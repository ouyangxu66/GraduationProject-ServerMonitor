package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.service.serverservice.IServerInfoService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server")
public class ServerInfoController {


    private IServerInfoService serverInfoService;

    public ServerInfoController(IServerInfoService serverInfoService){
        this.serverInfoService = serverInfoService;
    }

    /**
     * 获取所有服务器列表
     * GET /server/list
     */
    @GetMapping("/list")
    public R<List<ServerInfo>> list() {
        return R.ok(serverInfoService.list());
    }

    /**
     * 新增或更新服务器
     * POST /server/save
     */
    @PostMapping("/save")
    public R<String> save(@RequestBody ServerInfo serverInfo) {
        // saveOrUpdate 是 MP 提供的神器：
        // 如果传入的对象有 ID，它就执行 Update；如果没有 ID，它就执行 Insert。
        boolean success = serverInfoService.saveOrUpdate(serverInfo);
        return success ? R.ok("success") : R.fail("fail");
    }

    /**
     * 删除服务器
     * DELETE /server/delete?id=1
     */
    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam Long id) {
        boolean success = serverInfoService.removeById(id);
        return success ? R.ok("success") : R.fail("fail");
    }
}