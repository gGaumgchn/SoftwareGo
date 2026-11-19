package com.softwarego.selection.stage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.announcement.domain.AnnouncementStatus;

/**
 * PUT /manager/stages 响应。
 */
@JsonPropertyOrder({"changeNo", "stageSaved", "announcementStatus", "announcementId", "message"})
public record StageSaveResponse(
        String changeNo,
        boolean stageSaved,
        AnnouncementStatus announcementStatus,
        Long announcementId,
        String message
) {
}
