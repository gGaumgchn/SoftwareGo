package com.softwarego.selection.stage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.announcement.domain.AnnouncementStatus;

import java.time.LocalDateTime;

/**
 * GET /manager/stage-changes 列表项。
 */
@JsonPropertyOrder({"changeNo", "operatorId", "operatorName", "publishRequested",
        "announcementStatus", "draftTitle", "announcementId", "createdAt", "updatedAt"})
public record StageChangeResponse(
        String changeNo,
        Long operatorId,
        String operatorName,
        boolean publishRequested,
        AnnouncementStatus announcementStatus,
        String draftTitle,
        Long announcementId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
