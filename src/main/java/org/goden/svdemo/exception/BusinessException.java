package org.goden.svdemo.exception;

/**
 * 业务异常类
 * 用于在业务逻辑中抛出明确的异常信息
 */
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String message;

    /**
     * 错误详情（可选，用于调试）
     */
    private String detail;

    /**
     * 构造方法 - 仅错误信息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 1; // 默认业务错误码
        this.message = message;
    }

    /**
     * 构造方法 - 错误码和错误信息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    /**
     * 构造方法 - 错误码、错误信息和错误详情
     */
    public BusinessException(Integer code, String message, String detail) {
        super(message);
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    /**
     * 构造方法 - 错误信息+原因
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 1;
        this.message = message;
    }

    /**
     * 构造方法 - 错误码+错误信息+原因
     */
    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    /**
     * 快速创建异常的静态方法
     */
    public static BusinessException of(String message) {
        return new BusinessException(message);
    }

    public static BusinessException of(Integer code, String message) {
        return new BusinessException(code, message);
    }

    /**
     * 常用业务异常快速创建方法
     */
    public static BusinessException paramError(String message) {
        return new BusinessException(400, message);
    }

    public static BusinessException unauthorized() {
        return new BusinessException(401, "未授权");
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "权限不足");
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(404, message);
    }

    public static BusinessException systemError() {
        return new BusinessException(500, "系统错误");
    }

    // Getter 和 Setter
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
