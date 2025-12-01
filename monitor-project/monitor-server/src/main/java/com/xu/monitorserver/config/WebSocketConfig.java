package com.xu.monitorserver.config;

import com.xu.monitorserver.handler.WebSshWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;


@Configuration
@EnableWebSocket
// WebSocket 配置类,当有请求访问"/ws/ssh"时,交给WebSshWebSocketHandler来处理
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private WebSshWebSocketHandler webSshWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册处理路径，并允许跨域 (*)
        registry.addHandler(webSshWebSocketHandler, "/ws/ssh")
                .setAllowedOrigins("*");
    }
}