package com.softwarego.selection.stage.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * stage 实体。仅起止时间可更新，名称/编码/顺序固定。
 */
@Getter
@Setter
@NoArgsConstructor
public class Stage {

    private Long id;
    private StageCode stageCode;
    private String stageName;
    private Integer sortNo;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
}
