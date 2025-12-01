package com.xu.monitorserver.controller;

import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.service.serverservice.ServerInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/server")
public class ServerInfoController {

    @Autowired
    private ServerInfoService serverInfoService;

    /**
     * 获取所有服务器列表
     * GET /server/list
     */
    @GetMapping("/list")
    public List<ServerInfo> list() {
        return serverInfoService.list();
    }

    /**
     * 新增或更新服务器
     * POST /server/save
     */
    @PostMapping("/save")
    public String save(@RequestBody ServerInfo serverInfo) {
        // saveOrUpdate 是 MP 提供的神器：
        // 如果传入的对象有 ID，它就执行 Update；如果没有 ID，它就执行 Insert。
        boolean success = serverInfoService.saveOrUpdate(serverInfo);
        return success ? "success" : "fail";
    }

    /**
     * 删除服务器
     * DELETE /server/delete?id=1
     */
    @DeleteMapping("/delete")
    public String delete(@RequestParam Long id) {
        boolean success = serverInfoService.removeById(id);
        return success ? "success" : "fail";
    }
}