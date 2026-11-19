package com.softwarego.selection.stage.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.domain.StageState;
import com.softwarego.selection.stage.domain.StageStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /stages/current 响应。stages 固定包含 7 个阶段，供主页面步骤条使用。
 */
@JsonPropertyOrder({"serverTime", "status", "configured", "currentStage", "nextStage", "stages"})
public record CurrentStageResponse(
        LocalDateTime serverTime,
        StageStatus status,
        boolean configured,
        StageSummary currentStage,
        StageSummary nextStage,
        List<StageStateItem> stages
) {

    @JsonPropertyOrder({"stageCode", "stageName", "sortNo", "startAt", "endAt"})
    public record StageSummary(
            StageCode stageCode,
            String stageName,
            Integer sortNo,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
    }

    @JsonPropertyOrder({"stageCode", "stageName", "sortNo", "startAt", "endAt", "state"})
    public record StageStateItem(
            StageCode stageCode,
            String stageName,
            Integer sortNo,
            LocalDateTime startAt,
            LocalDateTime endAt,
            StageState state
    ) {
    }
}
