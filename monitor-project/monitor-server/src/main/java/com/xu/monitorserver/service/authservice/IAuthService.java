package com.xu.monitorserver.service.authservice;

import com.xu.monitorcommon.dto.RegisterDTO;
import org.springframework.transaction.annotation.Transactional;

public interface IAuthService {
    String login(String username, String password);

    @Transactional(rollbackFor = Exception.class) // 开启事务
    void register(RegisterDTO dto);
}