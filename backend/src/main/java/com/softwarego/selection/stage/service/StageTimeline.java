package com.softwarego.selection.stage.service;

import com.softwarego.selection.stage.domain.Stage;
import com.softwarego.selection.stage.domain.StageCode;
import com.softwarego.selection.stage.domain.StageState;
import com.softwarego.selection.stage.domain.StageStatus;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 只读时间线计算：当前阶段、空档、结束、未配置，以及每个阶段的状态。
 *
 * <p>阶段区间使用半开区间 [startAt, endAt)：恰好在 endAt 时视为已结束，
 * 以避免相邻阶段在边界上同时处于进行中。</p>
 */
public record StageTimeline(
        boolean configured,
        StageStatus status,
        Stage current,
        Stage next,
        Map<StageCode, StageState> states
) {

    public static StageTimeline of(List<Stage> stages, LocalDateTime now) {
        boolean configured = stages.size() == StageCode.values().length
                && stages.stream().allMatch(stage -> stage.getStartAt() != null && stage.getEndAt() != null);

        Map<StageCode, StageState> stateMap = new EnumMap<>(StageCode.class);
        for (Stage stage : stages) {
            stateMap.put(stage.getStageCode(), resolveState(stage, now));
        }

        Stage current = stages.stream()
                .filter(stage -> stateMap.get(stage.getStageCode()) == StageState.ONGOING)
                .findFirst()
                .orElse(null);

        Stage next = null;
        if (current == null) {
            next = stages.stream()
                    .filter(stage -> stateMap.get(stage.getStageCode()) == StageState.NOT_STARTED)
                    .findFirst()
                    .orElse(null);
        }

        StageStatus status;
        if (!configured) {
            status = StageStatus.UNCONFIGURED;
        } else if (current != null) {
            status = StageStatus.ONGOING;
        } else if (next != null) {
            status = StageStatus.BETWEEN;
        } else {
            status = StageStatus.ENDED;
        }

        // 未配置时不虚构当前/下一阶段。
        if (!configured) {
            current = null;
            next = null;
        }

        return new StageTimeline(configured, status, current, next, stateMap);
    }

    public StageState stateOf(Stage stage) {
        return states.getOrDefault(stage.getStageCode(), StageState.UNCONFIGURED);
    }

    private static StageState resolveState(Stage stage, LocalDateTime now) {
        if (stage.getStartAt() == null || stage.getEndAt() == null) {
            return StageState.UNCONFIGURED;
        }
        if (now.isBefore(stage.getStartAt())) {
            return StageState.NOT_STARTED;
        }
        if (now.isBefore(stage.getEndAt())) {
            return StageState.ONGOING;
        }
        return StageState.FINISHED;
    }
}
