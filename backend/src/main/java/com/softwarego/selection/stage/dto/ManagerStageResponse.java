package com.softwarego.selection.stage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.stage.domain.StageCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /manager/stages 响应：7 个固定阶段的当前配置。
 */
@JsonPropertyOrder({"serverTime", "configured", "currentStageCode", "stages"})
public record ManagerStageResponse(
        LocalDateTime serverTime,
        boolean configured,
        StageCode currentStageCode,
        List<StageItem> stages
) {

    @JsonPropertyOrder({"stageCode", "stageName", "sortNo", "startAt", "endAt"})
    public record StageItem(
            StageCode stageCode,
            String stageName,
            Integer sortNo,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
    }
}
