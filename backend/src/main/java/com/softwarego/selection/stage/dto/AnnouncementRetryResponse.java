package com.softwarego.selection.stage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.announcement.domain.AnnouncementStatus;

/**
 * POST /manager/stages/announcement 响应。
 */
@JsonPropertyOrder({"changeNo", "announcementStatus", "announcementId", "message"})
public record AnnouncementRetryResponse(
        String changeNo,
        AnnouncementStatus announcementStatus,
        Long announcementId,
        String message
) {
}
