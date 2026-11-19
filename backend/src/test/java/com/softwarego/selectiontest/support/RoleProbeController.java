package com.softwarego.selectiontest.support;

import com.softwarego.selection.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅测试使用：提供三个角色路径探针，验证 AuthInterceptor 的 401/403。
 * 位于测试源码，不会进入生产 jar。
 */
@RestController
@RequestMapping("/api/v1")
public class RoleProbeController {

    @GetMapping("/student/probe")
    public Result<String> studentProbe() {
        return Result.ok("STUDENT");
    }

    @GetMapping("/teacher/probe")
    public Result<String> teacherProbe() {
        return Result.ok("TEACHER");
    }

    @GetMapping("/manager/probe")
    public Result<String> managerProbe() {
        return Result.ok("MANAGER");
    }
}
