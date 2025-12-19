package com.xu.monitorserver.controller;

import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.service.serverservice.IServerInfoService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server")
public class ServerInfoController {

    private final IServerInfoService serverInfoService;

    public ServerInfoController(IServerInfoService serverInfoService) {
        this.serverInfoService = serverInfoService;
    }

    /**
     * 获取所有服务器列表
     * GET /server/list
     */
    @GetMapping("/list")
    public R<List<ServerInfo>> list() {
        String username = getCurrentUsername();
        return R.ok(serverInfoService.listForUser(username));
    }

    /**
     * 新增或更新服务器
     * POST /server/save
     */
    @PostMapping("/save")
    public R<String> save(@RequestBody ServerInfo serverInfo) {
        String username = getCurrentUsername();
        serverInfoService.saveForUser(username, serverInfo);
        return R.ok("保存成功");
    }

    /**
     * 删除服务器
     * DELETE /server/1
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable("id") Long id) {
        String username = getCurrentUsername();
        serverInfoService.deleteForUser(username, id);
        return R.ok("删除成功");
    }

    /**
     * 辅助方法:获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ServiceException(401, "未登录");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof String s) {
            // Spring Security 未登录时通常为 anonymousUser
            if ("anonymousUser".equalsIgnoreCase(s)) {
                throw new ServiceException(401, "未登录");
            }
            return s;
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        throw new ServiceException(401, "未登录");
    }
}