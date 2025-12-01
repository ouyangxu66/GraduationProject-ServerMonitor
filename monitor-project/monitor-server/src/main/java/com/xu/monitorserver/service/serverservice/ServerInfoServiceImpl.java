package com.xu.monitorserver.service.serverservice;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.monitorserver.entity.ServerInfo;
import com.xu.monitorserver.mapper.ServerInfoMapper;
import org.springframework.stereotype.Service;

@Service
public class ServerInfoServiceImpl extends ServiceImpl<ServerInfoMapper, ServerInfo> implements ServerInfoService {
    // MP 的 ServiceImpl 已经帮我们实现了大部分 CRUD 逻辑，
    // 所以这里暂时是空的，但它已经具备了 save, removeById, list 等能力。
}