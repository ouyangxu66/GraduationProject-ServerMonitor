package com.xu.monitorserver.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;
import com.xu.monitorserver.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Component
public class AliyunOssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.url-prefix}")
    private String urlPrefix;

    /**
     * 上传文件到 OSS
     * @param file 前端传来的文件
     * @return 文件的完整访问 URL
     */
    public String uploadFile(MultipartFile file) {
        // 1. 生成文件路径：avatar/2025/12/10/uuid.png
        String originalFilename = file.getOriginalFilename();
        String suffix = null;
        if (originalFilename != null) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String datePath = sdf.format(new Date());
        
        // 拼接文件名 (avatar 目录可自定义)
        String fileName = "avatar/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + suffix;

        // 2. 创建 OSS 客户端
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            InputStream inputStream = file.getInputStream();
            
            // 设置元信息 (防止浏览器直接下载图片，而是预览)
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(getContentType(suffix));
            
            // 3. 执行上传
            ossClient.putObject(bucketName, fileName, inputStream, meta);

            // 4. 返回完整 URL
            // 这里的 urlPrefix 在 yml 里配好了，直接拼接
            return urlPrefix + fileName;

        } catch (Exception e) {
            throw new ServiceException("OSS文件上传失败: " + e.getMessage());
        } finally {
            // 5. 关闭客户端
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    // 辅助方法：判断 ContentType
    private String getContentType(String suffix) {
        if (suffix.equalsIgnoreCase(".bmp")) return "image/bmp";
        if (suffix.equalsIgnoreCase(".gif")) return "image/gif";
        if (suffix.equalsIgnoreCase(".jpeg") || suffix.equalsIgnoreCase(".jpg")) return "image/jpeg";
        if (suffix.equalsIgnoreCase(".png")) return "image/png";
        return "application/octet-stream";
    }
}