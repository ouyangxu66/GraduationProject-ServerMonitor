package com.xu.monitorserver.exception;



/**
 * 自定义业务异常 (不使用 Lombok)
 * 用于在业务层主动抛出逻辑错误 (如: 用户已存在, 余额不足)
 */
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码 (设为 final，确保异常抛出后不可被修改)
     */
    private final Integer code;

    /**
     * 构造方法 1: 仅传递消息，默认为 500 错误
     * @param message 错误描述
     */
    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造方法 2: 传递错误码和消息
     * @param code 错误码 (如 400, 404, 403)
     * @param message 错误描述
     */
    public ServiceException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 手动实现 Getter 方法
     * 替代了 Lombok 的 @Getter
     */
    public Integer getCode() {
        return code;
    }

    // 异常类通常不需要 Setter，因为抛出后就不应该被修改了
    // 异常类通常也不需要重写 toString()，因为父类 Throwable 已经实现了很好的 toString()
}