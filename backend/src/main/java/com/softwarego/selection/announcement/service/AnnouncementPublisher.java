package com.softwarego.selection.announcement.service;

/**
 * 公告发布入口。生产实现使用独立事务；测试可通过 test sources 的
 * @Primary 实现注入失败，验证“阶段已保存、公告失败、按 changeNo 重试”。
 */
public interface AnnouncementPublisher {

    /**
     * 按 stage_change 的公告草稿发布公告；已发布时幂等返回已有公告。
     */
    AnnouncementPublishResult publish(String changeNo);

    /**
     * 公告发布失败后标记 stage_change 为 FAILED；已发布记录不降级。
     */
    void markFailed(String changeNo);
}
