package com.smarthome.rule.exception;

import com.smarthome.common.result.Result;
import com.smarthome.common.result.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * S2 局部异常处理：补齐 common 未覆盖的请求体解析/参数类型错误，
 * 使「非法 JSON」返回明确的 400 参数错误（而非兜底 500）。
 * 不处理 common 已覆盖的业务异常与参数校验异常。
 */
@RestControllerAdvice
public class RuleExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RuleExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("请求体解析失败: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "请求体不是合法 JSON");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("参数类型不匹配: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST.getCode(), "参数类型错误: " + e.getName());
    }
}
