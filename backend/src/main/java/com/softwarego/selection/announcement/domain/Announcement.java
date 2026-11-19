package com.softwarego.selection.announcement.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * announcement 实体。公告只增不改不删，仅由阶段保存的发布流程插入。
 */
@Getter
@Setter
@NoArgsConstructor
public class Announcement {

    private Long id;
    private String title;
    private String content;
    private Long publisherId;
    private LocalDateTime publishedAt;
}
