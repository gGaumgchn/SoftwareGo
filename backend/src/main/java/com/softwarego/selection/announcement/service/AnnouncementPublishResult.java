package com.softwarego.selection.announcement.service;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;

/**
 * 公告发布事务结果。
 */
public record AnnouncementPublishResult(
        String changeNo,
        AnnouncementStatus status,
        Long announcementId
) {
}
