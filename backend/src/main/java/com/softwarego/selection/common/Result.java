package com.softwarego.selection.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 统一响应体。
 *
 * <p>成功：{@code {code:200,message:"success",data:...}}；
 * 失败：{@code {code:非200,message:"...",data:null}}。</p>
 *
 * @param <T> 业务数据类型
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"code", "message", "data"})
public record Result<T>(int code, String message, T data) {

    public static final int SUCCESS_CODE = 200;

    public static <T> Result<T> ok(T data) {
        return new Result<>(SUCCESS_CODE, "success", data);
    }

    public static Result<Void> ok() {
        return new Result<>(SUCCESS_CODE, "success", null);
    }

    public static <T> Result<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
