package com.xu.monitorserver.service.sysuserservice;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.mapper.SysUserServiceMapper;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserServiceMapper, SysUser> implements SysUserService{
    // MP 的 ServiceImpl 已经帮我们实现了大部分 CRUD 逻辑，我们这里不需要再写任何代码
    // 如果需要自定义 SQL，只需要在这里编写方法，然后在 Mapper 文件中编写对应的 SQL 语句即可
}
