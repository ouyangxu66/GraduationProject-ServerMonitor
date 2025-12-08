package com.xu.monitorserver.handler;

import com.xu.monitorserver.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器，用于认证用户身份
 */
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(AuthHandshakeInterceptor.class);

    private final JwtUtils jwtUtils;

    public AuthHandshakeInterceptor(JwtUtils jwtUtils){
        this.jwtUtils=jwtUtils;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest){
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 从请求参数中获取Token
            String token = servletRequest.getServletRequest().getParameter("token");

            if (token != null && !token.isEmpty()){
                try {
                    //1.解析并校验Token
                    String username = jwtUtils.extractUsername(token);
                    if (username != null){
                        // 2. 将用户信息放入 WebSocket Session 属性中
                        // 后续在 WebSshWebSocketHandler 中可以通过 session.getAttributes().get("user") 获取
                        attributes.put("user",username);
                        return true;//允许连接
                    }
                } catch (Exception e) {
                    logger.warn("WebSocket 认证失败: Token无效或已过期");
                }
            }else {
                logger.warn("WebSocket 认证失败: 未携带Token");
            }
        }
        return false;//拒绝连接

    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    // do nothing
    }
}
