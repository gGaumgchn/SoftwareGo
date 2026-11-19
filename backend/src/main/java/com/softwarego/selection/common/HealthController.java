package com.softwarego.selection.common;

import com.softwarego.selection.common.dto.HealthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 公开健康检查。B1 引入 AuthInterceptor 后必须继续放行本接口。
 */
@RestController
@RequiredArgsConstructor
public class HealthController {

    private final Clock clock;

    @GetMapping("/health")
    public Result<HealthResponse> health() {
        LocalDateTime serverTime = LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        return Result.ok(new HealthResponse("UP", serverTime));
    }
}
