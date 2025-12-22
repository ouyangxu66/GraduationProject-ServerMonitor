package com.xu.monitorserver.exception;


import com.xu.monitorcommon.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException; // 如果引入了 Security
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理自定义业务异常
     * 场景：我们在 Service 中主动抛出的 throw new ServiceException("用户名已存在");
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 Spring Security 权限不足异常
     * 场景：普通用户访问管理员接口
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("权限不足: {}", e.getMessage());
        return R.fail(403, "当前操作没有权限");
    }

    /**
     * 处理 Spring Security 认证异常
     * 场景：用户名或密码错误
     */
    @ExceptionHandler(BadCredentialsException.class)
    public R<Void> handleBadCredentialsException(BadCredentialsException e) {
        // 🔴 打印这行日志，确认是否是密码错误
        logger.error("密码错误: {}", e.getMessage());
        return R.fail(401, "账号或密码错误");
    }

    /**
     * 处理 Spring Security 认证异常
      * 场景：认证服务异常
     */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public R<Void> handleInternalAuthException(InternalAuthenticationServiceException e) {
        logger.error("内部认证错误: {}", e.getMessage());
        return R.fail(500, "认证服务异常");
    }

    /**
     * 处理上传文件超过限制异常。
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public R<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        logger.warn("上传文件过大: {}", e.getMessage());
        // 413 更语义化，但项目统一用 R.code；这里返回 413 便于前端判断
        return R.fail(413, "上传文件过大，已超过服务器限制");
    }

    /**
     * 处理所有未知的运行时异常 (兜底策略)
     * 场景：空指针(NPE)、数组越界、数据库连接失败等不可预见的错误
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        // 打印堆栈信息，方便排查 BUG
        logger.error("系统未知异常", e);
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}