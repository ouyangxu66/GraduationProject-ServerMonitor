package com.xu.monitorserver.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类，用于生成和管理JSON Web Tokens
 * 提供token生成、签名和验证功能
 */
@Component
public class JwtUtils {
    
    /**
     * 从配置文件获取密钥字符串，用于生成JWT签名密钥
     * 需要足够长且复杂以确保安全性
     */
    @Value("${jwt.secret}")
    private String SECRET;

    /**
     * Token过期时间设置为24小时（毫秒）
     * 86400秒 = 24小时 * 60分钟 * 60秒 * 1000毫秒
     */
    private static final long EXPIRATION = 86400 * 1000L;


    /**
     * 使用预定义的密钥生成HMAC SHA密钥对象
     * 该密钥将用于JWT的签名和验证过程
     */
    private Key getKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    /**
     * 根据用户名生成JWT访问令牌
     * @param username 用户名，作为JWT的主题(subject)
     * @return 返回生成的JWT字符串
     */
    public String generateToken(String username){
        // 创建声明集合，可以添加自定义声明信息
        Map<String, Object> claims = new HashMap<>();
        // 调用创建token的方法
        return createToken(claims,username);
    }

    /**
     * 创建JWT token的核心方法
     * 
     * @param claims 自定义声明映射，可以包含用户角色、权限等额外信息
     * @param subject 主题，这里使用用户名作为主题标识
     * @return 返回构建完成并签名的JWT字符串
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                // 设置声明信息（可自定义字段）
                .setClaims(claims)
                // 设置主题（subject），通常是用户标识
                .setSubject(subject)
                // 设置签发时间
                .setIssuedAt(new Date(System.currentTimeMillis()))
                // 设置过期时间（当前时间+有效期）
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                // 使用指定密钥和算法进行签名
                .signWith(getKey(), SignatureAlgorithm.HS256)
                // 构建JWT并压缩为字符串
                .compact();
    }

    /**
     * 验证JWT令牌是否有效
     * 
     * @param token 待验证的JWT令牌字符串
     * @param username 需要与令牌中用户名匹配的用户名
     * @return 如果令牌有效且未过期则返回true，否则返回false
     */
    public boolean validateToken(String token, String username) {
        final String user = extractUsername(token);
        return (user.equals(username) && !isTokenExpired(token));
    }

    /**
     * 从JWT令牌中提取用户名
     * 
     * @param token JWT令牌字符串
     * @return 令牌中的用户名（主题）
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 解析JWT令牌并提取所有声明信息
     * 
     * @param token JWT令牌字符串
     * @return 包含令牌中所有声明信息的Claims对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 检查JWT令牌是否已过期
     * 
     * @param token JWT令牌字符串
     * @return 如果令牌已过期返回true，否则返回false
     */
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }
}
