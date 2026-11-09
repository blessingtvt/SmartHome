package com.smarthome.rule.client;

/**
 * 规则动作外部调用异常（S2-04）。
 *
 * <p>用于区分「评估成功但动作调用失败」：连接失败、超时、HTTP 非 200、
 * 业务 code 非 200、响应格式异常等，统一携带可读的失败原因。</p>
 */
public class ActionCallException extends RuntimeException {

    public ActionCallException(String message) {
        super(message);
    }

    public ActionCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
