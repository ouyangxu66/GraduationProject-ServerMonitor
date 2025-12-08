package com.xu.monitorserver;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码生成测试
 */
public class PasswordTest {

    @Test
    public void generatePassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // 生成 "123456" 的密文
        String result = encoder.encode("123456");
        System.out.println("--------------------------------------------------");
        System.out.println("请复制下面的密文更新到数据库:");
        System.out.println(result);
        System.out.println("--------------------------------------------------");
    }
}