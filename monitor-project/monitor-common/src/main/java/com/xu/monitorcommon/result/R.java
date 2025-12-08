package com.xu.monitorcommon.result;


import java.io.Serializable;

/**
 * 通用返回结果封装类
 * @param <T> 数据载体的类型
 */

public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 (200: 成功, 500: 失败, 401: 未认证...) */
    private int code;
    /** 返回消息 */
    private String msg;

    /** 数据对象 */
    private transient T data;

    // --- 构造方法 ---

    public R() {}

    public R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // --- 快捷静态方法 (Success) ---

    /**
     * 成功，无数据返回
     */
    public static <T> R<T> ok() {
        return new R<>(200, "操作成功", null);
    }

    /**
     * 成功，带数据返回
     */
    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data);
    }

    /**
     * 成功，自定义消息和数据
     */
    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data);
    }

    // --- 快捷静态方法 (Fail) ---

    /**
     * 失败，使用默认 500 状态码
     */
    public static <T> R<T> fail(String msg) {
        return new R<>(500, msg, null);
    }

    /**
     * 失败，自定义状态码和消息
     */
    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}