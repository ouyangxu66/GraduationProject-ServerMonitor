package com.xu.monitorserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xu.monitorserver.entity.ServerInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ServerInfoMapper extends BaseMapper<ServerInfo> {
    // 暂时不需要手写 SQL，MP 自带了 insert, deleteById, selectList 等方法
}