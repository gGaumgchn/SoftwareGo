package com.softwarego.selection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 统一时区与时钟。
 *
 * <p>业务代码必须通过注入的 {@link Clock} 获取当前时间，禁止散落
 * {@code LocalDateTime.now()}，这样才能在测试中固定时间边界。</p>
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    public static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE_ID);
    }
}
