package com.softwarego.selectiontest.walkthrough;

import com.softwarego.selectiontest.support.AnnouncementFailureToggleController;
import com.softwarego.selectiontest.support.FailingAnnouncementPublisherConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * B2 真实 HTTP 走查启动类：除生产组件外，额外注册可失败的公告发布器和开关。
 * 位于测试源码，不进入生产 jar。
 */
@SpringBootApplication(scanBasePackages = {
        "com.softwarego.selection.common",
        "com.softwarego.selection.config",
        "com.softwarego.selection.auth",
        "com.softwarego.selection.user",
        "com.softwarego.selection.stage",
        "com.softwarego.selection.announcement"
})
@MapperScan({
        "com.softwarego.selection.user.mapper",
        "com.softwarego.selection.stage.mapper",
        "com.softwarego.selection.announcement.mapper"
})
@Import({FailingAnnouncementPublisherConfig.class, AnnouncementFailureToggleController.class})
public class B2WalkthroughApplication {

    public static void main(String[] args) {
        SpringApplication.run(B2WalkthroughApplication.class, args);
    }
}
