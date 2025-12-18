package com.xu.monitorserver.config;

import com.xu.monitorserver.filter.JwtAuthenticationTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // 开启方法权限控制,加上此注解才能使@PreAuthorize 注解生效
public class SecurityConfiguration {

    private final JwtAuthenticationTokenFilter jwtAuthFilter;

    public SecurityConfiguration(JwtAuthenticationTokenFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 开启跨域 (CORS)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2. 禁用 CSRF (关键！否则 POST 请求会报 403)
                .csrf(AbstractHttpConfigurer::disable)
                // 3. 设置无状态 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. 配置拦截规则
                .authorizeHttpRequests(auth -> auth
                        // 🔴 关键：明确放行登录接口和注册账号接口，支持 POST 方法
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST,"/api/auth/register").permitAll()
                        // 放行 WebSocket
                        .requestMatchers("/ws/**").permitAll()
                        // 允许匿名访问 Agent 上报接口
                        .requestMatchers("/api/agent/**").permitAll()
                        // 放行 Client 上报接口
                        .requestMatchers("/api/monitor/report").permitAll()
                        // 允许匿名访问刷新接口
                        .requestMatchers("/api/auth/refresh").permitAll()
                        // 其他所有请求需要认证
                        .anyRequest().authenticated()
                )
                // 新增：配置异常处理器，处理未登录(401)的情况
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"code\":401,\"msg\":\"未授权，请登录\"}");
                        })
                )
                // 5. 添加 JWT 过滤器
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置全局 CORS (允许跨域)
     */
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // 允许所有来源 (开发环境方便，生产环境建议指定具体域名)
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}