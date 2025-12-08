package com.xu.monitorserver.config;

import com.xu.monitorserver.handler.AuthHandshakeInterceptor;
import com.xu.monitorserver.handler.WebSshWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;


@Configuration
@EnableWebSocket
// WebSocket 配置类,当有请求访问"/ws/ssh"时,交给WebSshWebSocketHandler来处理
public class WebSocketConfig implements WebSocketConfigurer {


    private final WebSshWebSocketHandler webSshWebSocketHandler;
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(WebSshWebSocketHandler webSshWebSocketHandler,
                           AuthHandshakeInterceptor authHandshakeInterceptor){
        this.webSshWebSocketHandler = webSshWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册处理路径，并允许跨域 (*)
        registry.addHandler(webSshWebSocketHandler, "/ws/ssh")
                .setAllowedOrigins("*")
                .addInterceptors(authHandshakeInterceptor)
                .addInterceptors(new HandshakeInterceptor() {
                    /**
                     * 握手之前
                     */
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request,
                                                   ServerHttpResponse response,
                                                   WebSocketHandler wsHandler,
                                                   Map<String, Object> attributes) throws Exception {
                        if(request instanceof ServletServerHttpRequest){
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            String token = servletRequest.getServletRequest().getParameter("token");

                            if (token!=null&&!token.isEmpty()){
                                return true;
                            }
                        }
                        return false;

                    }

                    /**
                     * 握手之后
                     */
                    @Override
                    public void afterHandshake(ServerHttpRequest request,
                                               ServerHttpResponse response,
                                               WebSocketHandler wsHandler,
                                               Exception exception) {
                        // do nothing
                    }
                });
    }
}