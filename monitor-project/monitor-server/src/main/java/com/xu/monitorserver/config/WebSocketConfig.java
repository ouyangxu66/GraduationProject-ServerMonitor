package com.xu.monitorserver.config;

import com.xu.monitorserver.handler.AuthHandshakeInterceptor;
import com.xu.monitorserver.handler.WebSshWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
// WebSocket 配置类,当有请求访问"/ws/ssh"时,交给WebSshWebSocketHandler来处理
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSshWebSocketHandler webSshWebSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(WebSshWebSocketHandler webSshWebSocketHandler,
                           AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.webSshWebSocketHandler = webSshWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册处理路径，并允许跨域 (*)
        registry.addHandler(webSshWebSocketHandler, "/ws/ssh")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor);
    }
}