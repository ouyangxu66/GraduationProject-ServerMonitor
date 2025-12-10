package com.xu.monitorserver.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 用户角色
 */
@TableName("sys_role")
public class SysRole {
    private long id; // 主键id
    private String roleName; // 角色名称
    private String roleCode; // 角色编码

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }
}
