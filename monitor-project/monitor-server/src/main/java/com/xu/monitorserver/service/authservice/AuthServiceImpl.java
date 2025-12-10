package com.xu.monitorserver.service.authservice;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.monitorcommon.dto.RegisterDTO;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.SysUserMapper;
import com.xu.monitorserver.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;


    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           SysUserMapper userMapper,
                           PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userMapper=userMapper;
        this.passwordEncoder=passwordEncoder;
    }

    @Override
    public String login(String username, String password) {
        // 1. 使用 AuthenticationManager 进行认证 (会自动调用 UserDetailsServiceImpl)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // 2. 认证通过后生成 Token
        if (authentication.isAuthenticated()) {
            return jwtUtils.generateToken(username);
        } else {
            throw new ServiceException("认证失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务
    public void register(RegisterDTO dto) {
        // 1. 基础校验
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new ServiceException("两次输入的密码不一致");
        }

        // 2. 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new ServiceException("用户名已存在");
        }

        // 3. 构建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        // 密码加密
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 默认角色
        user.setRole("ROLE_USER");
        user.setNickname("新用户");
        user.setDeleted(0); // 未删除

        userMapper.insert(user);
    }
}