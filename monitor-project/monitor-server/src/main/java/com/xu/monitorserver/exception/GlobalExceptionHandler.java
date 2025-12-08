package com.xu.monitorserver.exception;


import com.xu.monitorcommon.result.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException; // 如果引入了 Security
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 1. 处理自定义业务异常
     * 场景：我们在 Service 中主动抛出的 throw new ServiceException("用户名已存在");
     */
    @ExceptionHandler(ServiceException.class)
    public R<Void> handleServiceException(ServiceException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * 2. 处理 Spring Security 权限不足异常
     * 场景：普通用户访问管理员接口
     */
    @ExceptionHandler(AccessDeniedException.class)
    public R<Void> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("权限不足: {}", e.getMessage());
        return R.fail(403, "当前操作没有权限");
    }

    /**
     * 3. 处理 Spring Security 认证失败异常
     * 场景：Token 无效或过期
     */
    @ExceptionHandler(AuthenticationException.class)
    public R<Void> handleAuthenticationException(AuthenticationException e) {
        logger.warn("认证失败: {}", e.getMessage());
        return R.fail(401, "认证失败，请重新登录");
    }

    /**
     * 4. 处理所有未知的运行时异常 (兜底策略)
     * 场景：空指针(NPE)、数组越界、数据库连接失败等不可预见的错误
     */
    @ExceptionHandler(Exception.class)
    public R<Void> handleException(Exception e) {
        // 打印堆栈信息，方便排查 BUG
        logger.error("系统未知异常", e);
        return R.fail(500, "系统繁忙，请稍后再试");
    }
}