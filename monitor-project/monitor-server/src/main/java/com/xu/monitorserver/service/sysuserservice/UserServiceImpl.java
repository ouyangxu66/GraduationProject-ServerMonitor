package com.xu.monitorserver.service.sysuserservice;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xu.monitorcommon.dto.UserProfileDTO;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.mapper.SysUserMapper;
import com.xu.monitorserver.utils.AliyunOssUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserServiceImpl implements IUserService {

    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private AliyunOssUtil aliyunOssUtil;

    public UserServiceImpl(SysUserMapper sysUserMapper,
                           PasswordEncoder passwordEncoder,
                           AliyunOssUtil aliyunOssUtil){
        this.passwordEncoder=passwordEncoder;
        this.userMapper=sysUserMapper;
        this.aliyunOssUtil=aliyunOssUtil;
    }
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
        // 1. 基础校验
        if (file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }

        // 校验文件大小 (例如限制 2MB)
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new ServiceException("图片大小不能超过2MB");
        }

        // 2. 调用 OSS 工具类上传
        // 所有的 IO 异常处理、文件名生成都在 Util 里做好了
        String avatarUrl = aliyunOssUtil.uploadFile(file);

        // 3. 更新数据库
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

    @Override
    public void deleteAccount(String password) {
        String username = getCurrentUsername();
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));

        // 1. 验证密码 (注销是高风险操作，必须验证)
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new ServiceException("密码错误，无法注销");
        }

        // 2. 逻辑删除 (更新 deleted 字段)
        user.setDeleted(1);
        userMapper.updateById(user);

        // 或者物理删除: userMapper.deleteById(user.getId());
    }
}