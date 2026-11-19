package com.softwarego.selection.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阶段配置开关。
 */
@Component
@ConfigurationProperties(prefix = "app.stage")
@Getter
@Setter
public class StageProperties {

    /**
     * 是否允许延长已截止阶段：旧 end_at 已到达/已过时，新 end_at 仍可晚于旧值。
     * 默认关闭，保持“阶段调整不重新开放已截止填报”的业务约束；
     * 仅本地演示时可在 application-local.yml 中开启。
     */
    private boolean allowExtendEnded = false;
}
