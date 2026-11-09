package com.smarthome.common.exception;

/**
 * 业务异常。Service 层抛出，由 {@link GlobalExceptionHandler} 统一转换为响应。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(String message) {
        this(400, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
