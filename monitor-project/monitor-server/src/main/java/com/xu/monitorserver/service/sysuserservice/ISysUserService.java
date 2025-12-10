package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xu.monitorserver.entity.SysUser;

public interface ISysUserService extends IService<SysUser> {
    // 继承 IService 可以直接使用 MP 封装好的业务方法
}
