package com.softwarego.selectiontest.support;

import com.softwarego.selection.announcement.service.AnnouncementPublishService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试专用配置：用可失败发布器替换生产 AnnouncementPublisher 的注入。
 * 只在导入本配置的 @SpringBootTest 中生效，不影响生产 jar。
 */
@TestConfiguration
public class FailingAnnouncementPublisherConfig {

    @Bean
    @Primary
    public FailingAnnouncementPublisher failingAnnouncementPublisher(
            AnnouncementPublishService announcementPublishService) {
        return new FailingAnnouncementPublisher(announcementPublishService);
    }
}
