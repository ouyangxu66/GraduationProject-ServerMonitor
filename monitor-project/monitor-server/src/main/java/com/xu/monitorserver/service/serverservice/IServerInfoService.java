package com.xu.monitorserver.service.serverservice;



import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.monitorserver.entity.ServerInfo;

import java.util.List;

// 继承 IService 可以直接使用 MP 封装好的业务方法
public interface IServerInfoService extends IService<ServerInfo> {
    /**
     * 查询当前用户创建的服务器列表（按创建时间倒序），并填充在线状态。
     */
    List<ServerInfo> listForUser(String username);

    /**
     * 新增或更新服务器（会校验是否属于当前用户）。
     */
    void saveForUser(String username, ServerInfo serverInfo);

    /**
     * 删除服务器（会校验是否属于当前用户）。
     */
    void deleteForUser(String username, Long id);
}