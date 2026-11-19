package com.softwarego.selectiontest.support;

import com.softwarego.selection.file.service.LocalResumeFileStorage;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试专用存储配置。只在导入本配置的 @SpringBootTest 中生效。
 */
@TestConfiguration
public class FailingResumeFileStorageConfig {

    @Bean
    @Primary
    public FailingResumeFileStorage failingResumeFileStorage(
            LocalResumeFileStorage localResumeFileStorage) {
        return new FailingResumeFileStorage(localResumeFileStorage);
    }
}
