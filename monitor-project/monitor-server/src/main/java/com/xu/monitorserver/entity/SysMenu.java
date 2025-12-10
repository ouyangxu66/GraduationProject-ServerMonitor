package com.xu.monitorserver.entity;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("sys_menu")
public class SysMenu {
    private long id; // 主键id
    private String menuName; // 菜单名称
    private String perms; // 权限标识
    private String type; // 菜单类型

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}