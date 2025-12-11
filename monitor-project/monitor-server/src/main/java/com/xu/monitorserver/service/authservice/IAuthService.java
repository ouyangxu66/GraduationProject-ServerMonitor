package com.xu.monitorserver.service.authservice;

import com.xu.monitorcommon.dto.RegisterDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

public interface IAuthService {
    Map<String, String> login(String username, String password);

    @Transactional(rollbackFor = Exception.class) // 开启事务
    void register(RegisterDTO dto);

    Map<String, String> refreshToken(String refreshToken);
}