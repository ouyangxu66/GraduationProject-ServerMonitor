package com.xu.monitorserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.monitorserver.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {
    
    // 通过角色编码查询对应的权限标识
    @Select("SELECT m.perms FROM sys_menu m " +
            "LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "LEFT JOIN sys_role r ON rm.role_id = r.id " +
            "WHERE r.role_code = #{roleCode} AND m.perms IS NOT NULL")
    List<String> selectPermsByRoleCode(String roleCode);
}