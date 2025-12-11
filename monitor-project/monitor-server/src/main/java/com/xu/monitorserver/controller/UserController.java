package com.xu.monitorserver.controller;

import com.xu.monitorcommon.dto.UserProfileDTO;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.service.sysuserservice.IUserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final IUserService userService;

    public UserController(IUserService userService){
        this.userService = userService;
    }

    /**
     * 获取个人资料 (包含权限列表)
     */
    @GetMapping("/profile")
    public R<SysUser> getProfile() {
        // 1. 调用 Service 获取基本用户信息 (从数据库查)
        SysUser user = userService.getUserProfile();

        // 2. 🟢 核心修改：从 Spring Security 上下文中获取当前登录用户的权限列表
        // 这些权限是在登录时由 UserDetailsServiceImpl 加载的
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            List<String> perms = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            // 3. 将权限列表填充到 User 对象中传给前端
            // (请确保 SysUser 实体类中已添加了 private List<String> permissions 字段)
            user.setPermission(perms);
        }

        return R.ok(user);
    }

    /**
     * 更新基本信息
     */
    @PutMapping("/profile")
    public R<Void> updateProfile(@RequestBody UserProfileDTO dto) {
        userService.updateUserProfile(dto);
        return R.ok();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    public R<Void> updatePassword(@RequestBody Map<String, String> params) {
        String oldPwd = params.get("oldPassword");
        String newPwd = params.get("newPassword");

        // Controller 层只做简单的非空校验，业务校验交给 Service
        if (oldPwd == null || newPwd == null) {
            return R.fail("参数不完整");
        }

        userService.updatePassword(oldPwd, newPwd);
        return R.ok();
    }

    /**
     * 头像上传
     */
    @PostMapping("/avatar")
    public R<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String avatarUrl = userService.uploadAvatar(file);
        return R.ok(avatarUrl);
    }

    /**
     * 检查密码是否正确
     */
    @PostMapping("/check-password")
    public R<Boolean> checkPassword(@RequestBody Map<String,String> params){
        String password = params.get("password");
        boolean result = userService.checkPassword(password);
        if (result){
            return R.ok(true);
        } else {
            return R.fail("旧密码错误,请重新输入!");
        }
    }

    /**
     * 注销账号
     */
    @PostMapping("/delete-account")
    public R<Void> deleteAccount(@RequestBody Map<String, String> params) {
        String password = params.get("password");
        if (password == null) return R.fail("请输入密码");

        userService.deleteAccount(password);
        return R.ok();
    }
}