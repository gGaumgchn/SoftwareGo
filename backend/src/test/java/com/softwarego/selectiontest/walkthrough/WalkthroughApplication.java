package com.softwarego.selectiontest.walkthrough;

import com.softwarego.selectiontest.support.RoleProbeController;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * 测试专用启动类，供 B1/B2 真实 HTTP 走查使用。
 * 位于测试源码，不会进入生产 jar。
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
@Import(RoleProbeController.class)
public class WalkthroughApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalkthroughApplication.class, args);
    }
}
