package com.xu.monitorserver.config;

import com.xu.monitorserver.handler.AuthHandshakeInterceptor;
import com.xu.monitorserver.handler.WebSshWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSSH WebSocket 端点配置。
 *
 * <p>职责：把 WebSocket 的访问路径（如 <code>/ws/ssh</code>）路由到具体的处理器 {@link WebSshWebSocketHandler}。
 * 同时在握手阶段挂载拦截器 {@link AuthHandshakeInterceptor}，用于做登录态校验/权限校验等（具体逻辑以拦截器实现为准）。</p>
 *
 * <p>说明：这里的 WebSocket 只负责“浏览器 ↔ 服务端”的长连接通信；真正的 SSH 连接由后端
 * {@link com.xu.monitorserver.service.sshservice.ISshService} 使用 JSch 建立。</p>
 */
@Configuration
@EnableWebSocket
// WebSocket 配置类,当有请求访问"/ws/ssh"时,交给WebSshWebSocketHandler来处理
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSSH 的业务处理器：负责解析前端消息、建立 SSH 连接、转发终端输入输出。
     */
    private final WebSshWebSocketHandler webSshWebSocketHandler;

    /**
     * 握手拦截器：在 WebSocket 握手阶段执行，可用于校验 Token/Cookie、注入用户信息到 session attributes 等。
     */
    private final AuthHandshakeInterceptor authHandshakeInterceptor;

    public WebSocketConfig(WebSshWebSocketHandler webSshWebSocketHandler,
                           AuthHandshakeInterceptor authHandshakeInterceptor) {
        this.webSshWebSocketHandler = webSshWebSocketHandler;
        this.authHandshakeInterceptor = authHandshakeInterceptor;
    }

    /**
     * 注册 WebSocket 处理器与拦截器。
     *
     * <p>路径约定：</p>
     * <ul>
     *   <li><code>/ws/ssh</code>：WebSSH 终端连接入口</li>
     * </ul>
     *
     * <p>跨域：当前配置允许所有来源（<code>*</code>）。如果你后续要上线生产环境，建议改成白名单来源，
     * 以避免被第三方站点滥用 WebSocket 端点。</p>
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSshWebSocketHandler, "/ws/ssh")
                // 允许跨域访问（开发/演示方便）；生产环境建议改为指定域名
                .setAllowedOrigins("*")
                // 握手拦截器：可在握手时做鉴权/绑定用户信息
                .addInterceptors(authHandshakeInterceptor);
    }
}