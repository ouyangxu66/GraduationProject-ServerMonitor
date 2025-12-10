package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xu.monitorserver.mapper.SysMenuMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情服务实现类
 * 实现Spring Security的UserDetailsService接口，用于加载用户特定数据
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {
    /**
     * 系统用户Mapper，用于访问数据库中的用户信息
     */
    private final SysUserMapper sysUserMapper;
    private SysMenuMapper sysMenuMapper;
    
    /**
     * 构造函数注入SysUserMapper依赖
     * @param sysUserMapper 系统用户数据访问对象
     */
    public UserDetailServiceImpl(SysUserMapper sysUserMapper,
                                 SysMenuMapper sysMenuMapper){
        this.sysUserMapper=sysUserMapper;
        this.sysMenuMapper=sysMenuMapper;
    }

    /**
     * 根据用户名加载用户详细信息
     * @param username 需要查找的用户名
     * @return UserDetails对象，包含用户的认证和授权信息
     * @throws UsernameNotFoundException 当用户不存在时抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 查询用户 (过滤掉已删除的)
        SysUser sysUser = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)); // 🟢 关键：只查没被注销的

        if (sysUser == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2. 动态查询权限
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 2.1 添加角色 (ROLE_ADMIN)
        authorities.add(new SimpleGrantedAuthority(sysUser.getRole()));

        // 2.2 添加具体权限 (server:add, server:list)
        // 🟢 核心：根据角色去 sys_menu 表查权限
        List<String> perms = sysMenuMapper.selectPermsByRoleCode(sysUser.getRole()); // 🟢 核心：根据角色去 sys_menu 表查权限
        for (String perm : perms) {
            if (perm != null && !perm.isEmpty()) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        // 3. 返回 Security User
        return new User(sysUser.getUsername(), sysUser.getPassword(), authorities);
    }
}
