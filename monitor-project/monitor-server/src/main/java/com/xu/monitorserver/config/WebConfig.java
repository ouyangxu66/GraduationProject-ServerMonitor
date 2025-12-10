package com.xu.monitorserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${monitor.upload.path}")
    private String uploadPath;

    @Value("${monitor.upload.mapping-path}")
    private String mappingPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 关键：将 URL 映射到 本地文件系统
        // addResourceLocations 要求必须以 file: 开头
        registry.addResourceHandler(mappingPath)
                .addResourceLocations("file:" + uploadPath);
    }
}