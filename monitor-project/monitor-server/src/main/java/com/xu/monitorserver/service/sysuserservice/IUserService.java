package com.xu.monitorserver.service.sysuserservice;

import com.xu.monitorcommon.dto.UserProfileDTO;
import com.xu.monitorserver.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService {
    
    /**
     * 获取个人资料
     */
    SysUser getUserProfile();

    /**
     * 更新基本信息
     */
    void updateUserProfile(UserProfileDTO dto);

    /**
     * 修改密码
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void updatePassword(String oldPassword, String newPassword);

    /**
     * 上传头像
     * @param file 文件对象
     * @return 图片访问URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 校验密码
     * @param rawPassword 原始密码
     * @return ture or false
     */
    boolean checkPassword(String rawPassword);

    void deleteAccount(String password);
}