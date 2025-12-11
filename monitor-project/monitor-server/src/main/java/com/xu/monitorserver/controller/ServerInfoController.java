package com.xu.monitorserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.mapper.ServerInfoMapper;
import com.xu.monitorserver.service.serverservice.IServerInfoService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/server")
public class ServerInfoController {


    private final ServerInfoMapper serverInfoMapper;
    private IServerInfoService serverInfoService;

    public ServerInfoController(IServerInfoService serverInfoService, ServerInfoMapper serverInfoMapper){
        this.serverInfoService = serverInfoService;
        this.serverInfoMapper = serverInfoMapper;
    }

    /**
     * 获取所有服务器列表
     * GET /server/list
     */
    @GetMapping("/list")
    public R<List<ServerInfo>> list() {
        String username = getCurrentUsername();
        List<ServerInfo> list = serverInfoMapper.selectList(
                new LambdaQueryWrapper<ServerInfo>()
                        .eq(ServerInfo::getCreateBy,username)
                        .orderByDesc(ServerInfo::getCreateTime)
        );
        return R.ok(list);
    }

    /**
     * 新增或更新服务器
     * POST /server/save
     */
    @PostMapping("/save")
    public R<String> save(@RequestBody ServerInfo serverInfo) {
        String username = getCurrentUsername();

        // 新增
        if (serverInfo.getId() == null){
            //绑定服务器所有者
            serverInfo.setCreateBy(username);
            serverInfoMapper.insert(serverInfo);
        }else {
            // 更新,校验服务器是否属于当前用户(防止恶意删除他人数据)
            ServerInfo old = serverInfoMapper.selectById(serverInfo.getId());
            if (old == null || !old.getCreateBy().equals(username)){
                return R.fail("无权修改此服务器");
            }
        }

        serverInfo.setCreateBy(username);
        serverInfoMapper.updateById(serverInfo);

        return R.ok("保存成功");
    }

    /**
     * 删除服务器
     * DELETE /server/1
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable("id") Long id) {
        String username = getCurrentUsername();
        ServerInfo old = serverInfoMapper.selectById(id);
        if (old == null)return R.ok();

        if (!old.getCreateBy().equals(username)){
            return R.fail("无权删除此服务器");
        }
        serverInfoService.removeById(id);
        return R.ok("删除成功");
    }

    /**
     * 辅助方法:获取当前登录用户名
     * 当对服务器进行操作时先验证当前登录用户是否有权限
     */
    private String getCurrentUsername(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof String) {
            return (String) principal;
        } else {
            // 假设 principal 是 UserDetails 的实现类
            return ((UserDetails) principal).getUsername();
        }
    }
}