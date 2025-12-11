package com.xu.monitorserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.entity.SysUser;
import com.xu.monitorserver.mapper.SysUserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/user")
@PreAuthorize("hasRole('ADMIN')") // 管理员权限才能查看
public class UserManageController {

    private SysUserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    public UserManageController(SysUserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 分页获取用户列表
     */
    @GetMapping("/list")
    public R<IPage<SysUser>> list(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                  @RequestParam(value = "size", defaultValue = "10")Integer size,
                                  @RequestParam(value = "username", required = false )String username){
        Page<SysUser> pageParm = new Page<>(page,size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();

        // 按用户名模糊查询
        wrapper.like(username != null && !username.isBlank(),SysUser::getUsername,username);
        // 过滤掉已逻辑删除的
        wrapper.eq(SysUser::getDeleted,0);
        // 按创建时间倒序
        wrapper.orderByDesc(SysUser::getCreateTime);
        // 查询
        IPage<SysUser> result = userMapper.selectPage(pageParm,wrapper);
        // 密码脱敏
        result.getRecords().forEach(sysUser -> sysUser.setPassword(null));

        return R.ok(result);
    }

    /**
     * 添加用户
     */
    @PostMapping("/add")
    public R<String> add(@RequestBody SysUser user){
        // 检查用户名是否已存在
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, user.getUsername()));

        if (count > 0)return R.fail("用户名已存在");

        // 设置默认密码 123456
        user.setPassword(passwordEncoder.encode("123456"));
        // 设置删除状态
        user.setDeleted(0);
        // 设置默认角色 ROLE_USER
        if (user.getRole() == null)user.setRole("ROLE_USER");

        userMapper.insert(user);
        return R.ok();
    }

    /**
     * 修改用户
     */
    @PutMapping("/update")
    public R<Void> update(@RequestBody SysUser user){
        // 密码不在这里进行修改,这里只更新非 null 字段
        user.setPassword(null);
        userMapper.updateById(user);
        return R.ok();
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/delete")
    public R<Void> delete(@RequestParam Long id){
        SysUser user = new SysUser();
        user.setId(id);
        user.setDeleted(1);
        userMapper.updateById(user);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @PostMapping("/reset-pwd/{id}")
    public R<Void> resetPwd(@PathVariable Long id){
        SysUser user = new SysUser();
        user.setId(id);
        //重置为初始密码 123456
        user.setPassword(passwordEncoder.encode("123456"));
        userMapper.updateById(user);
        return R.ok();
    }
}
