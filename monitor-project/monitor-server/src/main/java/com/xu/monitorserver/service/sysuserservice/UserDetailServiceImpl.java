package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.User;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务实现类
 * 实现Spring Security的UserDetailsService接口，用于加载用户特定数据
 */
public class UserDetailServiceImpl implements UserDetailsService {
    /**
     * 系统用户Mapper，用于访问数据库中的用户信息
     */
    private final SysUserMapper sysUserMapper;
    
    /**
     * 构造函数注入SysUserMapper依赖
     * @param sysUserMapper 系统用户数据访问对象
     */
    public UserDetailServiceImpl(SysUserMapper sysUserMapper){
        this.sysUserMapper=sysUserMapper;
    }

    /**
     * 根据用户名加载用户详细信息
     * @param username 需要查找的用户名
     * @return UserDetails对象，包含用户的认证和授权信息
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //1.使用QueryWrapper查询指定用户名的系统用户
        SysUser sysUser = sysUserMapper.selectOne(
                new QueryWrapper<SysUser>().eq("username", username));
        // 检查用户是否存在
        if (sysUser == null){
            // 抛出用户名未找到异常
            throw new UsernameNotFoundException("用户不存在:"+username);
        }
        //2.解析用户角色和权限
        List<GrantedAuthority> authorities=new ArrayList<>();
        if (sysUser.getRole() != null && !sysUser.getRole().isEmpty()){
            authorities.add(new SimpleGrantedAuthority(sysUser.getRole()));
        }
        //3.返回Spring Security 标准的User 对象
        //User其实是UserDetails接口的一个具体实现类
        return new User(
                sysUser.getUsername(),
                sysUser.getPassword(),
                authorities
        );
    }
}
