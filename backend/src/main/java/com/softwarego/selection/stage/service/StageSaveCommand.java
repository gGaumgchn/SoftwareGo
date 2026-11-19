package com.softwarego.selection.stage.service;

import com.softwarego.selection.stage.domain.StageCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 已通过结构与时间校验、按 sortNo 排序的阶段保存命令。
 */
public record StageSaveCommand(
        String changeNo,
        List<StageTimeCommand> stages,
        boolean publishAnnouncement,
        String announcementTitle,
        String announcementContent
) {

    public record StageTimeCommand(
            StageCode stageCode,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
    }
}
