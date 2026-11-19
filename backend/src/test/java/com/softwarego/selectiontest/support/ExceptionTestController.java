package com.softwarego.selectiontest.support;

import com.softwarego.selection.common.BusinessException;
import com.softwarego.selection.common.ErrorCode;
import com.softwarego.selection.common.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试使用：构造各类异常，验证 GlobalExceptionHandler 的 code / HTTP 状态。
 * 放在主应用扫描包之外，避免影响生产组件扫描。
 */
@RestController
@RequestMapping("/test/errors")
public class ExceptionTestController {

    @PostMapping("/validation")
    public Result<Void> validation(@Valid @RequestBody ValidationRequest request) {
        return Result.ok();
    }

    @GetMapping("/conflict")
    public Result<Void> conflict() {
        throw new BusinessException(ErrorCode.CONFLICT, "名额已满");
    }

    @GetMapping("/forbidden")
    public Result<Void> forbidden() {
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @GetMapping("/unauthorized")
    public Result<Void> unauthorized() {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    @GetMapping("/not-found")
    public Result<Void> notFound() {
        throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @GetMapping("/file-error")
    public Result<Void> fileError() {
        throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR, "简历上传失败，请重试");
    }

    @GetMapping("/unknown")
    public Result<Void> unknown() {
        throw new IllegalStateException("boom");
    }

    public record ValidationRequest(@NotBlank(message = "学工号异常") String staffNo) {
    }
}
