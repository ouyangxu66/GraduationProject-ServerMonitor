package com.xu.monitorserver.service.authservice;


import com.xu.monitorserver.exception.ServiceException;
import com.xu.monitorserver.utils.JwtUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public AuthServiceImpl(AuthenticationManager authenticationManager, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }

    @Override
    public String login(String username, String password) {
        // 1. 使用 AuthenticationManager 进行认证 (会自动调用 UserDetailsServiceImpl)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        // 2. 认证通过后生成 Token
        if (authentication.isAuthenticated()) {
            return jwtUtils.generateToken(username);
        } else {
            throw new ServiceException("认证失败");
        }
    }
}