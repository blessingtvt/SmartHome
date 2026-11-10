package com.smarthome.rule.client;

/**
 * 自动指令被 S3 手动控制保护拦截（业务码 409）。
 *
 * <p>与 {@link ActionCallException}（动作调用失败）区分：本异常表示「条件已命中、动作被保护机制
 * 有意拦截」，规则应记 BLOCKED（而非 FAILED），并在保护期结束后允许重新触发。</p>
 */
public class ManualProtectedException extends RuntimeException {

    public ManualProtectedException(String message) {
        super(message);
    }
}
