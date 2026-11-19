package com.softwarego.selection.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/**
 * 全局异常处理：所有错误响应保持 {@code {code,message,data}} 结构。
 *
 * <p>业务异常使用 {@link BusinessException} 携带的 HTTP 状态；参数校验、状态冲突、
 * 文件失败等业务类错误使用 HTTP 200 + 非 200 code；认证/权限/404/500 使用对应 HTTP 状态。
 * 未捕获异常只返回通用文案，绝不向客户端暴露堆栈、SQL 或内部实现。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(
            BusinessException exception, HttpServletRequest request) {
        log.warn("业务异常: method={}, path={}, code={}, message={}",
                request.getMethod(), request.getRequestURI(), exception.getCode(), exception.getMessage());
        return build(exception.getHttpStatus(), exception.getCode(), exception.getMessage());
    }

    /**
     * 同时覆盖 {@code MethodArgumentNotValidException}（它是 {@link BindException} 的子类）。
     * 只返回第一条可读错误，文案由 DTO 上的 message 决定。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElseGet(() -> exception.getBindingResult().getGlobalErrors().stream()
                        .map(ObjectError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .filter(text -> !text.isBlank())
                        .findFirst()
                        .orElse(ErrorCode.INVALID_PARAMETER.getMessage()));
        return fail(ErrorCode.INVALID_PARAMETER, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElse(ErrorCode.INVALID_PARAMETER.getMessage());
        return fail(ErrorCode.INVALID_PARAMETER, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleNotReadable(HttpMessageNotReadableException exception) {
        return fail(ErrorCode.INVALID_PARAMETER, "请求参数格式错误");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        return fail(ErrorCode.INVALID_PARAMETER, "缺少必要参数：" + exception.getParameterName());
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<Result<Void>> handleMissingPathVariable(MissingPathVariableException exception) {
        return fail(ErrorCode.INVALID_PARAMETER, "缺少必要路径参数：" + exception.getVariableName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return fail(ErrorCode.INVALID_PARAMETER, "参数格式错误：" + exception.getName());
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<Result<Void>> handleUnsupportedRequest(Exception exception) {
        return fail(ErrorCode.INVALID_PARAMETER, "请求格式不支持");
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(Exception exception) {
        return fail(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 唯一索引兜底失败等并发冲突。具体业务应尽量先做状态判断并给出精确文案。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        // 只记录异常类型，避免把 SQL、索引名或参数值写入日志。
        log.warn("数据完整性冲突: type={}", exception.getClass().getSimpleName());
        return fail(ErrorCode.CONFLICT, "操作冲突，请刷新后重试");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return fail(ErrorCode.FILE_UPLOAD_ERROR, "简历上传失败，请重试");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknownException(
            Exception exception, HttpServletRequest request) {
        log.error("未捕获异常: method={}, path={}", request.getMethod(), request.getRequestURI(), exception);
        return fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<Result<Void>> fail(ErrorCode errorCode, String message) {
        return build(errorCode.getHttpStatus(), errorCode.getCode(), message);
    }

    private ResponseEntity<Result<Void>> build(HttpStatus httpStatus, int code, String message) {
        return ResponseEntity.status(httpStatus).body(Result.fail(code, message));
    }
}
