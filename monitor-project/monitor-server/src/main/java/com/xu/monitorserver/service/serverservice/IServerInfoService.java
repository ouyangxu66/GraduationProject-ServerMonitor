package com.xu.monitorserver.service.serverservice;



import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.monitorserver.entity.ServerInfo;

// 继承 IService 可以直接使用 MP 封装好的业务方法
public interface IServerInfoService extends IService<ServerInfo> {
    // 这里以后可以扩展自定义的业务方法，比如 "校验IP是否重复"
}