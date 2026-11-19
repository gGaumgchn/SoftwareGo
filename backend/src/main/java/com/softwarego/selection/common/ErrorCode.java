package com.softwarego.selection.common;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。
 *
 * <p>业务校验/状态冲突类错误使用 HTTP 200 + 非 200 code，便于前端统一弹提示；
 * 认证、权限、资源不存在、未捕获异常使用对应 HTTP 状态码，响应体仍保持统一结构。</p>
 */
public enum ErrorCode {

    SUCCESS(200, "success", HttpStatus.OK),

    INVALID_PARAMETER(40001, "参数校验失败", HttpStatus.OK),

    UNAUTHORIZED(40101, "未登录或会话过期", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(40301, "无权访问该数据", HttpStatus.FORBIDDEN),

    NOT_FOUND(40401, "资源不存在", HttpStatus.NOT_FOUND),

    CONFLICT(40901, "操作冲突", HttpStatus.OK),

    INTERNAL_ERROR(50001, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),

    FILE_UPLOAD_ERROR(50002, "文件上传/存储失败", HttpStatus.OK);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
