package com.softwarego.selection.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * <p>业务校验/状态冲突默认使用 HTTP 200；认证、权限、资源不存在等通过
 * {@link ErrorCode} 携带对应 HTTP 状态。Controller 不写 try/catch，
 * 统一由 {@link GlobalExceptionHandler} 转换。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode.getCode(), message, errorCode.getHttpStatus());
    }

    /**
     * 业务校验/状态冲突默认 HTTP 200，例如名额已满、重复确认。
     */
    public BusinessException(int code, String message) {
        this(code, message, HttpStatus.OK);
    }

    public BusinessException(int code, String message, HttpStatus httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
