package com.xu.monitorserver.service.authservice;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.monitorcommon.dto.RegisterDTO;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.entity.SysUserToken;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.SysUserMapper;
import com.xu.monitorserver.mapper.SysUserTokenMapper;
import com.xu.monitorserver.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private SysUserTokenMapper userTokenMapper;


    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           JwtUtils jwtUtils,
                           SysUserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           SysUserTokenMapper userTokenMapper) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userMapper=userMapper;
        this.passwordEncoder=passwordEncoder;
        this.userTokenMapper=userTokenMapper;
    }

    @Override
    public Map<String, String> login(String username, String password) {
        // 1. 校验账号密码 (原逻辑)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        if (authentication.isAuthenticated()) {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

            // 2. 生成 Access Token (短)
            String accessToken = jwtUtils.createToken(username, JwtUtils.ACCESS_EXPIRE);

            // 3. 生成 Refresh Token (长)
            String refreshToken = jwtUtils.createToken(username, JwtUtils.REFRESH_EXPIRE);

            // 4. 保存 Refresh Token 到数据库 (存在则更新，不存在则插入)
            SysUserToken userToken = new SysUserToken();
            userToken.setUserId(user.getId());
            userToken.setRefreshToken(refreshToken);
            userToken.setExpireTime(LocalDateTime.now().plusDays(7));

            if (userTokenMapper.selectById(user.getId()) == null) {
                userTokenMapper.insert(userToken);
            } else {
                userTokenMapper.updateById(userToken);
            }

            // 5. 返回双 Token
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            return tokens;
        }
        throw new ServiceException("认证失败");
    }

    // 🟢 新增：刷新 Token 逻辑
    public Map<String, String> refreshToken(String refreshToken) {
        // 1. 校验 Refresh Token 格式
        String username;
        try {
            username = jwtUtils.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ServiceException("Refresh Token 无效");
        }

        // 2. 校验数据库中的 Refresh Token 是否匹配 (防止黑客拿旧的 Token 伪造)
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        SysUserToken dbToken = userTokenMapper.selectById(user.getId());

        if (dbToken == null || !dbToken.getRefreshToken().equals(refreshToken)) {
            throw new ServiceException("Refresh Token 已失效，请重新登录");
        }

        if (dbToken.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException("Refresh Token 已过期，请重新登录");
        }

        // 3. 生成全新的一对 Token (Token 轮换机制，更安全)
        String newAccess = jwtUtils.createToken(username, JwtUtils.ACCESS_EXPIRE);
        String newRefresh = jwtUtils.createToken(username, JwtUtils.REFRESH_EXPIRE);

        // 4. 更新数据库
        dbToken.setRefreshToken(newRefresh);
        dbToken.setExpireTime(LocalDateTime.now().plusDays(7));
        userTokenMapper.updateById(dbToken);

        Map<String, String> map = new HashMap<>();
        map.put("accessToken", newAccess);
        map.put("refreshToken", newRefresh);
        return map;
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