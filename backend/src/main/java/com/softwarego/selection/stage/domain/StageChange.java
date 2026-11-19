package com.softwarego.selection.stage.domain;

import com.softwarego.selection.announcement.domain.AnnouncementStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * stage_change 实体：阶段保存幂等记录 + 公告草稿/发布状态。
 */
@Getter
@Setter
@NoArgsConstructor
public class StageChange {

    private Long id;
    private String changeNo;
    private Long operatorId;
    private Boolean publishRequested;
    private AnnouncementStatus announcementStatus;
    private String draftTitle;
    private String draftContent;
    private Long announcementId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
