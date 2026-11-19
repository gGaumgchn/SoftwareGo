package com.softwarego.selectiontest.support;

import com.softwarego.selection.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仅 B2 真实 HTTP 走查使用：控制下一次公告发布是否失败。
 * 位于测试源码，不进入生产 jar。
 */
@RestController
@RequestMapping("/api/v1/test/announcement")
public class AnnouncementFailureToggleController {

    private final FailingAnnouncementPublisher publisher;

    public AnnouncementFailureToggleController(FailingAnnouncementPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/fail-next")
    public Result<Void> failNext() {
        publisher.failNextPublish();
        return Result.ok();
    }

    @PostMapping("/reset")
    public Result<Void> reset() {
        publisher.reset();
        return Result.ok();
    }
}
