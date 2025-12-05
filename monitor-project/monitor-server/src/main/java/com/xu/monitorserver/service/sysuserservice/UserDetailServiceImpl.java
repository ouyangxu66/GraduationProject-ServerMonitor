package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.User;
import java.util.Collections;

/**
 * 用户详情服务实现类
 * 实现Spring Security的UserDetailsService接口，用于加载用户特定数据
 */
public class UserDetailServiceImpl implements UserDetailsService {
    /**
     * 日志记录器实例
     */
    private static final Logger logger= LoggerFactory.getLogger(UserDetailServiceImpl.class);
    
    /**
     * 系统用户Mapper，用于访问数据库中的用户信息
     */
    private final SysUserMapper sysUserMapper;
    
    /**
     * 构造函数注入SysUserMapper依赖
     * 
     * @param sysUserMapper 系统用户数据访问对象
     */
    public UserDetailServiceImpl(SysUserMapper sysUserMapper){
        this.sysUserMapper=sysUserMapper;
    }

    /**
     * 根据用户名加载用户详细信息
     * 
     * @param username 需要查找的用户名
     * @return UserDetails对象，包含用户的认证和授权信息
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 使用QueryWrapper查询指定用户名的系统用户
        SysUser sysUser = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", username));
        // 检查用户是否存在
        if (sysUser == null){
            // 记录用户不存在的错误日志
            logger.error("用户不存在:{}", username);
            // 抛出用户名未找到异常
            throw new UsernameNotFoundException("用户不存在:"+username);
        }
        // 返回包含用户名、密码和权限列表的UserDetails实现
        return new User(sysUser.getUsername(),sysUser.getPassword(), Collections.emptyList());
    }
}
