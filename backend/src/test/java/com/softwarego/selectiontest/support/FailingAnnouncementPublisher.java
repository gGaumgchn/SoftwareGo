package com.softwarego.selectiontest.support;

import com.softwarego.selection.announcement.service.AnnouncementPublishResult;
import com.softwarego.selection.announcement.service.AnnouncementPublishService;
import com.softwarego.selection.announcement.service.AnnouncementPublisher;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 测试专用公告发布器：可让下一次发布直接失败，用于验证
 * “阶段已保存、公告失败、按 changeNo 重试”的完整链路。
 *
 * <p>成功路径仍委托真实 AnnouncementPublishService，因此公告插入、
 * 行锁和 PUBLISHED 回填都是真实事务。</p>
 */
public class FailingAnnouncementPublisher implements AnnouncementPublisher {

    private final AnnouncementPublishService delegate;
    private final AtomicBoolean failNext = new AtomicBoolean(false);

    public FailingAnnouncementPublisher(AnnouncementPublishService delegate) {
        this.delegate = delegate;
    }

    @Override
    public AnnouncementPublishResult publish(String changeNo) {
        if (failNext.compareAndSet(true, false)) {
            throw new IllegalStateException("simulated announcement publish failure");
        }
        return delegate.publish(changeNo);
    }

    @Override
    public void markFailed(String changeNo) {
        delegate.markFailed(changeNo);
    }

    public void failNextPublish() {
        failNext.set(true);
    }

    public void reset() {
        failNext.set(false);
    }
}
