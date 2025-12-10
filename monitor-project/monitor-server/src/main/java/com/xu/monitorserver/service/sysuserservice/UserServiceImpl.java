package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xu.monitorcommon.dto.UserProfileDTO;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {

    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    public UserServiceImpl(SysUserMapper sysUserMapper,PasswordEncoder passwordEncoder){
        this.passwordEncoder=passwordEncoder;
        this.userMapper=sysUserMapper;
    }

    //从 yaml 注入文件上传路径
    @Value("${monitor.upload.path}")
    private String uploadPath;

    // 辅助方法：获取当前登录用户名
    /**
     * 修复后的获取当前登录用户名方法
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new ServiceException("未获取到登录信息");
        }

        Object principal = authentication.getPrincipal();

        // 情况1：Principal 是 UserDetails 对象 (标准做法)
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        // 情况2：Principal 是字符串 (某些简单框架或匿名访问)
        if (principal instanceof String) {
            return (String) principal;
        }

        throw new ServiceException("获取用户信息失败，Principal类型未知");
    }

    @Override
    public SysUser getUserProfile() {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, getCurrentUsername()));
        if (user != null) {
            user.setPassword(null); // 脱敏，不返回密码给前端
        }
        return user;
    }

    @Override
    public void updateUserProfile(UserProfileDTO dto) {
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUsername, getCurrentUsername())
                .set(SysUser::getNickname, dto.getNickname())
                .set(SysUser::getEmail, dto.getEmail())
                .set(SysUser::getBio, dto.getBio()));
    }

    @Override
    public void updatePassword(String oldPassword, String newPassword) {
        String username = getCurrentUsername();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        // 业务逻辑：校验旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceException("旧密码错误");
        }

        // 业务逻辑：更新新密码
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, user.getId())
                .set(SysUser::getPassword, passwordEncoder.encode(newPassword)));
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 1. 校验
        if (file.isEmpty()) {
            // 直接抛出业务异常，GlobalExceptionHandler 会捕获并返回 500 给前端
            throw new ServiceException("上传文件不能为空");
        }

        // 2. 准备目录
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs(); // 自动创建多级目录
        }

        // 3. 生成唯一文件名 (防止重名覆盖)
        // 提取后缀名 (如 .png)
        String originalFilename = file.getOriginalFilename();
        String suffix = null;
        if (originalFilename != null) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + suffix;

        File dest = new File(dir, fileName);

        // 4. 保存文件 (核心解答区域)
        try {
            // 执行保存
            file.transferTo(dest);
        } catch (IOException e) {
            // 🔴 这里的 try-catch 是为了将 "底层技术异常" 转换为 "业务异常"
            // 这样 Controller 不需要关心什么是 IOException，只知道"业务失败了"
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }

        // 5. 生成访问 URL
        // 注意：生产环境这里通常是域名，这里为了演示用 localhost + 映射路径
        // 假设映射路径是 /images/**
        String avatarUrl = "http://localhost:8080/images/" + fileName;

        // 6. 更新数据库
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUsername, getCurrentUsername())
                .set(SysUser::getAvatar, avatarUrl));

        return avatarUrl;
    }


    @Override
    public boolean checkPassword(String rawPassword) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, getCurrentUsername()));
        return passwordEncoder.matches(rawPassword,user.getPassword());
    }
}