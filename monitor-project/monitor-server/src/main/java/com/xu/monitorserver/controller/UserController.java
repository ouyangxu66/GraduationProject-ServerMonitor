package com.xu.monitorserver.controller;


import com.xu.monitorcommon.dto.UserProfileDTO;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.service.sysuserservice.IUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private IUserService userService;

    public UserController(IUserService userService){
        this.userService=userService;
    }

    /**
     * 获取个人资料
     */
    @GetMapping("/profile")
    public R<SysUser> getProfile() {
        return R.ok(userService.getUserProfile());
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
        }else {
            return R.fail("旧密码错误,请重新输入!");
        }
    }
}