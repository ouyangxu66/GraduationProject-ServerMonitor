package com.xu.monitorserver.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xu.monitorcommon.result.R;
import com.xu.monitorserver.utils.JwtUtils;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 用于拦截HTTP请求，从Authorization头中提取JWT令牌并验证用户身份
 * 继承OncePerRequestFilter确保每个请求只被处理一次
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    /**
     * 日志记录器实例
     */
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationTokenFilter.class);


    /**
     * 用户详情服务，用于加载用户信息
     */
    private final UserDetailsService userDetailsService;
    
    /**
     * JWT工具类实例，用于处理JWT令牌的生成和验证
     */
    private final JwtUtils jwtUtils;

    /**
     * 构造函数，用于初始化JwtUtils和UserDetailsService
     * 
     * @param jwtUtils JWT工具类实例
     * @param userDetailsService 用户详情服务实例
     */
    public JwtAuthenticationTokenFilter(JwtUtils jwtUtils,UserDetailsService userDetailsService){
        this.jwtUtils=jwtUtils;
        this.userDetailsService=userDetailsService;
    }
    
    /**
     * 过滤器核心方法，处理每个HTTP请求的身份验证
     * 
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @param filterChain 过滤器链，用于继续处理请求
     * @throws ServletException Servlet异常
     * @throws IOException IO异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        //1.获取Header中的Authorization
        final String authHeader=request.getHeader("Authorization");
        String username=null;
        String jwt=null;

        // 检查Authorization头部是否存在且以Bearer开头
        if (authHeader != null && authHeader.startsWith("Bearer")){
            // 提取JWT令牌（去掉"Bearer "前缀）
            jwt=authHeader.substring(7);
            try {
                // 从JWT令牌中提取用户名
                username = jwtUtils.extractUsername(jwt);
            }catch (ExpiredJwtException e){
                logger.warn("JWT已过期: {}", e.getMessage());
                response401(response, "Token已过期，请刷新");
                return; // ⛔ 终止过滤器链，不再往后走
            } catch (Exception e) {
                // 记录JWT解析失败的日志
                logger.warn("JWT解析Username失败:{}",e.getMessage());
                response401(response, "Token无效");
            }
        }

        //2.校验Token并设置SecurityContext
        // 如果用户名存在且当前上下文中没有认证信息
        if(username != null && SecurityContextHolder.getContext().getAuthentication() == null){
            // 加载用户详细信息,再次确认用户是否存在
            UserDetails userDetails =this.userDetailsService.loadUserByUsername(username);
            // 验证JWT令牌有效性
            if (jwtUtils.validateToken(jwt,userDetails.getUsername())){
                // 创建认证令牌
                UsernamePasswordAuthenticationToken authToken=new UsernamePasswordAuthenticationToken(
                        userDetails,null,userDetails.getAuthorities());

                // 设置认证详细信息
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //核心:设置当前登录用户
                // 将认证信息存入安全上下文
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        //3.放行请求
        // 继续执行过滤器链中的下一个过滤器
        filterChain.doFilter(request,response);
    }
    // 辅助方法：手动写入 JSON 响应
    private void response401(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(401); // HTTP 状态码
        response.setContentType("application/json;charset=UTF-8");
        // 使用 R 类封装返回结果，code=401
        String json = new ObjectMapper().writeValueAsString(R.fail(401, msg));
        response.getWriter().write(json);
    }
}
